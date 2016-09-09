package ch.menneri.smail

import com.typesafe.config.ConfigFactory
import java.io.File
import javax.mail._
import javax.mail.internet.InternetAddress
import net.fortuna.mstor._


trait SmailSupport {

  def acquireAccount(service: String, port: Int, account: String, password: String, folder: Option[String], localDirectory: String) {

    val props = System.getProperties
    val session = Session.getDefaultInstance(props, null)
    val store = session.getStore("imaps")
    
    try {
      store.connect(service, port, account, password)
    }  catch {
      case a: AuthenticationFailedException => {
        println(a.getMessage)
        System.exit(1)
      }
    }
    
    store.isConnected match {
      case true => println("* connected to imap server")
      case false => {
        println("* could not connect to imap server")
        System.exit(1)
      }
    }

    val mailRoot = store.getDefaultFolder

    folder match {
      case f: Some[String] => {
        println(s"* Acquiring folder ${f.get} from email account: $account")
        val fldr = f.get
        acqFolder(mailRoot.getFolder(fldr), localDirectory)
      }
      case None => {
        println(s"* Acquiring all folders from email account: $account")
        acqAllFolders(localDirectory)
      }
    }

    def acqAllFolders(localDirectory: String) {
      val folders = mailRoot.list("*")
      folders.foreach { folderName => acqFolder(folderName, localDirectory) }
    }

    def acqFolder(fldr: Folder, localDirectory: String) {
      val imapFolder = fldr.getName
      fldr.open(Folder.READ_ONLY)
      val count = fldr.getMessageCount
      val messages = fldr.getMessages
      println(s"* writing $count messages to local mbox file")   
      val mboxLocation = getMboxLocation(localDirectory, account, imapFolder)
      val mbox = new Mbox(mboxLocation)
      val mboxFolder = mbox.getMbox(imapFolder)
      var i = 1
      messages.foreach { message => 
        if(i % 25 == 0) { println(s"* writing message $i/$count") }
        mboxFolder.appendMessages(Array(message))
        i = i + 1
      }
      mbox.close  
    }
    
    store.close
  }
  
  def getMboxLocation(root: String, addressee: String, folder: String): File = {
    
    val rootMbox = new File(root)

    rootMbox.exists match {
      case true =>  println(s"* Directory $rootMbox exists.") 
      case false => {
        println(s"* Root folder doesn't exist, creating $rootMbox")
        rootMbox.mkdir
      }
    }

    val addr = addressee.split("@")(0) + "_AT_" + addressee.split("@")(1).split("\\.")(0) + "_DOT_" + addressee.split("@")(1).split("\\.")(1)
    val accountFolder = new File(rootMbox, addr)
    
    accountFolder.exists match {
      case true => println(s"* Folder for account $addr exists.")
      case false => {
        println(s"* Account folder doesn't exist, creating directory $addr.")
        accountFolder.mkdir
      }
    }
    val localMbox = new File(accountFolder, folder)

    localMbox.exists match {
      case true => println(s"* Local mbox file $folder exists.")
      case false => { 
        println(s"* Local mbox file doesn't exist, creating $folder.")
        localMbox.createNewFile
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
