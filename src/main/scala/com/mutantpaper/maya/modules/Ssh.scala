package com.mutantpaper.maya.modules

import java.io.File
import java.nio.file.{Files, Paths}

import akka.actor.{Actor, ActorLogging, Props}
import com.mutantpaper.maya.Messages._
import fr.janalyse.ssh.SSH
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.read

object Ssh {
  def props(): Props = {
    Props(new Ssh())
  }

  case class ServerConfig(alias: String, host: String, user: String, pass: String)
  case class SafeServerConfig(alias: String, host: String, user: String)
}
class Ssh() extends MModule {
  import Ssh._
  implicit val formats = DefaultFormats

  val standardIn = System.console()

  // Load config from file
  val configFile = new File("ssh.json")

  var servers: Map[String, ServerConfig] = if(configFile.exists()){
    read[Set[SafeServerConfig]](new String(Files.readAllBytes(Paths.get(configFile.getPath))))
      .map { s =>
        println(s"Input key for ${s.alias}:")
        val pass: String = new String(standardIn.readPassword())
        ServerConfig(s.alias, s.host, s.user, pass)
      }
      .map(s => (s.alias,s)).toMap
  } else {
    Map.empty[String, ServerConfig]
  }

  def execute(arguments: List[String]): String = arguments match {
    case alias :: command :: Nil =>
      servers.get(alias) match {
        case Some(sc) =>
          log.debug(s"Ssh module received command: $command for $alias")
          SSH.once(sc.host, sc.user, sc.pass) { ssh =>
            ssh.execute(command)
          }
        case None =>
          log.warning(s"Ssh module received command ($command) for unknown host ($alias)")
          "error"
      }
    case _ => "error"
  }

  val name = "ssh"
  val methods = Map("execute" -> execute _)

  log.info(s"$name module started")
}
