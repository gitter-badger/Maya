package com.mutantpaper.maya.modules

import akka.actor.{Actor, ActorLogging}
import com.mutantpaper.maya.Messages.{Call, Operation}

trait MModule extends Actor with ActorLogging{
  val name: String
  val methods: Map[String,  (List[String]) => String]

  def invoke(operation: Operation): Option[Operation] = {
    val Call(module, method, arguments) = operation.current
    operation.next(methods(method)(operation.current.getArguments(operation.arguments)))
  }

  def receive: Receive = {
    case op: Operation =>
      invoke(op) match {
        case Some(nextOp) => context.system.actorSelection("user/" + nextOp.current.module) ! nextOp
        case None => log.debug(s"finished op ($op)")
      }

    case msg =>
      log.warning(s"$name module received unexpected message ($msg)")
  }

  override def postStop(): Unit = {
    log.info(s"$name module stopped")
  }
}
