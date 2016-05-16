package ch.menneri.smails

import com.typesafe.config.ConfigFactory
import java.io.File
import javax.mail._
import javax.mail.internet.InternetAddress

import net.fortuna.mstor._

object Main extends App {
  val conf = ConfigFactory.load()

  val service = args(0)
  val account = args(1)
  val password = args(2)

  validateAddr(account) match {
    case false => System.exit(1)
    case _ =>
  }

  val props = System.getProperties
  val session = Session.getDefaultInstance(props, null)
  val store = session.getStore("imaps")
  store.connect(conf.getString(s"smail.$service"), conf.getInt("smail.port"), account, password)
  
  store.isConnected match {
    case true => println("* connected to imap server")
    case false => {
      println("* could not connect to imap server")
      System.exit(1)
    }
  }

  val mailRoot = store.getDefaultFolder
  val folders = mailRoot.list("*")

  folders.foreach{ folder =>
    val imapFolder = folder.getName
    folder.open(Folder.READ_ONLY)
    val count = folder.getMessageCount
    val messages = folder.getMessages
    println(s"* writing $count messages to local mbox file")   
    val mboxLocation = getMboxLocation("mbox", account, imapFolder)
    val mbox = new Mbox(mboxLocation)
    val mboxFolder = mbox.getMbox(imapFolder)
    var i = 1
    messages.foreach { message => 
      println(s"* writing message $i/$count" + message.getSubject)
      mboxFolder.appendMessages(Array(message))
      i = i + 1
    }
    mbox.close
  }
  
  store.close

  def validateAddr(addr: String): Boolean = {
    val emailAddr = new InternetAddress(addr)
    try { 
      emailAddr.validate
      true
    } catch {
      case e: Exception => {
        println("** email address provided is not valid")
        false
      }
    }
  }
    
  def getMboxLocation(root: String, addressee: String, folder: String): File = {
    
    val rootMbox = new File(root)
    if(rootMbox.exists == false) rootMbox.createNewFile

    val addr = addressee.split("@")(0) + "_AT_" + addressee.split("@")(1).split("\\.")(0) + "_DOT_" + addressee.split("@")(1).split("\\.")(1)
    val accountFolder = new File(rootMbox, addr)
    accountFolder.exists match {
      case true => println(s"* folder for account $addr exists")
      case false => {
        accountFolder.mkdir
        println(s"* account folder created for $addr")
      }
    }
    val localMbox = new File(accountFolder, folder)

    localMbox.exists match {
      case true => println(s"* local mbox file $folder already exists")
      case false => { 
        localMbox.createNewFile
        println(s"* local mbox file $folder created")
      }
    }

    accountFolder
  }
}

class Mbox(location: File) {

  val mProps = System.getProperties
  mProps.setProperty("mstor.mbox.metadataStrategy", "none")
  val mSession = Session.getDefaultInstance(mProps)
  val mStore = new MStorStore(mSession, new URLName("mstor:" + location.getAbsolutePath)) 
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
