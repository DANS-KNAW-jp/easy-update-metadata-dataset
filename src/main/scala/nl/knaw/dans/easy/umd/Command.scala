/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.umd

import java.io.File

import com.yourmediashelf.fedora.client.FedoraClient
import com.yourmediashelf.fedora.client.request.FedoraRequest
import nl.knaw.dans.easy.umd.{CommandLineOptions => cmd}
import org.apache.commons.csv.{CSVFormat, CSVParser}
import org.apache.commons.io.Charsets
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.util.{Failure, Success, Try}
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, Text}

object Command {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.debug("Starting command line interface")
    val ps = cmd.parse(args)
    FedoraRequest.setDefaultClient(new FedoraClient(ps.fedoraCredentials))
    run(ps) match {
      case _: Success[_] => log.info ("success")
      case e: Failure[_] => log.error (s"failed", e)
    }
  }

  def run(implicit ps: Parameters): Try[Unit] = {
    log.debug(s"running with $ps")

    for {
      list <- parse(ps.input)
      _ <- Try{list.foreach(update)}
    } yield ()
  }

  def update(record: Record) = Try {
    log.info(s"${record.fedoraPid}, ${record.newValue}")
    //TODO apply transform and update fedora stream
  }

  def parse(file: File): Try[List[Record]] = Try {
    CSVParser
      .parse(file, Charsets.UTF_8, CSVFormat.RFC4180)
      .filter(_.nonEmpty).drop(1)
      .map(csvRecord => new Record(csvRecord.get(0), csvRecord.get(0))).toList
  }

  def transform(label: String, newValue: String) = {
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(prefix, `label`, attribs, scope, children) =>
          Elem(prefix, label, attribs, scope, false, Text(newValue))
        case other => other
      }
    })
  }
}
