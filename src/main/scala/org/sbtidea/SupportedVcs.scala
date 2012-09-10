package org.sbtidea

import sbt.{Process, IO, Logger}
import java.io.File

object SupportedVcs {

  val ideaFilesToIgnore = ".idea/\n.idea_modules/"

  sealed abstract class VCS(val name: String) {
    def doIgnore(logger: Logger)

    def ignore(logger: Logger) {
      logger.info("Ignoring generated files using %s".format(name))
      doIgnore(logger)
      logger.success("Generated files have been ignored")
    }

    override def toString = name
  }

  case object UnknownVCS extends VCS("") {
    override def doIgnore(logger: Logger) {
      logger.warn(
        "The project doesn't use vcs or the vcs is not currently supported, generated files have not been ignored." +
          " If you vcs is not supported you still can ignore using 'ignore-generated-by-idea' argument")
    }

    override def ignore(logger: Logger) {
      doIgnore(logger)
    }
  }

  case object SVN extends VCS("svn") {
    override def doIgnore(logger: Logger) {
      IO.withTemporaryFile("svn:ignore", "") {
        f =>
          val toIgnore = (Process("svn propget svn:ignore .") !!) + ideaFilesToIgnore
          IO.append(f, toIgnore)
          (Process("svn propset svn:ignore -F %s .".format(f.getAbsolutePath)) !!)
      }
    }
  }

  case object GIT extends VCS("Git") {

    override def doIgnore(logger: Logger) {
      IO.append(new File(".", ".gitignore"), ideaFilesToIgnore)
    }
  }

  def list = List[VCS](SVN, GIT)
}