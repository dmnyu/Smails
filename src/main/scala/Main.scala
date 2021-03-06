package ch.menneri.smail

import com.typesafe.config.ConfigFactory
import javax.mail.internet.InternetAddress
import org.rogach.scallop._
import pl.project13.scala.rainbow._

class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val service = opt[String](required = true)
  val email = opt[String](required = true)
  val folder = opt[String](required = false)
  val local = opt[String](required = false)
  verify()
}


object Main extends App with SmailSupport {

  println("smail v.0.1")

  val c = new java.io.File("imap.conf")
  val conf = ConfigFactory.parseFile(c)
  val cli = new CLIConf(args)
  
  validateAddr(cli.email()) match {
    case false => println("email address is malformed"); System.exit(1)
    case _ =>
  }

  val service = cli.service.get.get
  val url = conf.getString(s"$service.url")
  val port = conf.getInt(s"$service.port")
  val password = getPassword
  
  
  cli.local.get match {
    case rootDirectory: Some[String] => acquireAccount(url, port, cli.email(), password, cli.folder.get, rootDirectory.get)
    case None => acquireAccount(url, port, cli.email(), password, cli.folder.get, "mbox")
  }

  def getPassword(): String = {
    println(s"enter the password for ${cli.email()}")
    Console.out.print("" + Console.INVISIBLE)
    val password = scala.io.StdIn.readLine()
    Console.out.print("" + Console.RESET)
    password
  }

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
  
}
    

