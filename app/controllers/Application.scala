package controllers

import scala.io._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.concurrent._
import play.api.Play.current

import Execution.Implicits.defaultContext

object Application extends Controller {

  def index = TODO

  def route(path: String) = Action { request =>
    Ok(request.toString)
  }
}

