package ch.menneri.jmail

import com.typesafe.config.ConfigFactory
import java.io.File
import java.util.Properties
import javax.mail._
import net.fortuna.mstor._

object Main extends App {
  val conf = ConfigFactory.load()
  val imapFolder= args(0)
  val service = args(1)
  val props = System.getProperties
  val mbox = new Mbox

  val session = Session.getDefaultInstance(props, null)
  val store = session.getStore("imaps")
  store.connect(conf.getString(s"smail.$service"), conf.getInt("smail.port"), args(2), args(3))
  
  store.isConnected match {
    case true => println("* connected to imap server")
    case false => {
      println("* could not connect to imap server")
      System.exit(1)
    }
  }
  
  val localMbox = (new File("mbox", imapFolder))

  localMbox.exists match {
    case true => println("* local mbox file already exists")
    case false => { 
      localMbox.createNewFile
      println("* local mbox file created")
    }
  }

  val mboxFolder = mbox.getMbox(imapFolder)

  val folder = store.getFolder(imapFolder)
  folder.open(Folder.READ_ONLY)
  val count = folder.getMessageCount
  val messages = folder.getMessages
  
  println(s"* writing $count messages to local mbox file")
  
  var i = 1
  messages.foreach { message => 
    println(s"* writing message $i/$count")
    mboxFolder.appendMessages(Array(message))
    i = i + 1
  }

  println(s"* $count messages written to local mbox file")
  

  mbox.close
  store.close
}

class Mbox {
  val mProps = System.getProperties
  mProps.setProperty("mstor.mbox.metadataStrategy", "none")
  val mSession = Session.getDefaultInstance(mProps)
  val mStore = new MStorStore(mSession, new URLName("mstor:mbox")) 
  mStore.connect
  val mboxRoot = mStore.getDefaultFolder

  def close() {
    mStore.close
  }

  def getMbox(name: String): Folder = {
    val mFolder = mboxRoot.getFolder(name)
    mFolder.open(Folder.READ_WRITE)
    mFolder
  }
}