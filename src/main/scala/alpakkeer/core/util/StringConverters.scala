package alpakkeer.core.util

object StringConverters {

  implicit class Converter(s: String) {

    implicit def toCamelCase: String = {
      var s_ = s

      s_ = "([a-z])([A-Z])".r.replaceAllIn(s_, m => m.group(1) + " " + m.group(2))
      s_ = "^[\\W]+".r.replaceAllIn(s_, "")
      s_ = "^[A-Za-z][a-z0-9]".r.replaceAllIn(s_, m => m.group(0).toLowerCase())
      s_ = "([^a-zA-Z\\d])([a-zA-Z])".r.replaceAllIn(s_, m => m.group(2).capitalize)
      s_ = "[^a-zA-Z\\d]+$".r.replaceAllIn(s_, "")
      s_ = "[^a-zA-Z\\d]+".r.replaceAllIn(s_, "")
      s_ = "([\\d])([A-Z])".r.replaceAllIn(s_, m => m.group(1) + m.group(2).toLowerCase)

      s_
    }

    implicit def toCapitalizedCamelCase: String = {
      toCamelCase.capitalize
    }

    implicit def toKebabCase: String = {
      "([a-z0-9])([A-Z])".r.replaceAllIn(toCamelCase, m => m.group(1) + "-" + m.group(2).toLowerCase)
    }

    implicit def toSnakeCase: String = {
      if (s.matches("^[a-z0-9_]+$")) {
        s
      } else {
        "([a-z0-9])([A-Z])".r.replaceAllIn(toCamelCase, m => m.group(1) + "_" + m.group(2).toLowerCase)
      }
    }

  }

  implicit class DataTable(data: List[List[String]]) {

    def toAsciiTable(hasHeader: Boolean = true): String = {
      val columnsCount = data.map(_.size).max
      val dataWithEmptyCells = data.map(_.padTo(columnsCount, ""))
      val colWidths = dataWithEmptyCells.transpose.map(_.map(_.length).max).map(_ + 4)

      def row(row: List[String], padWith: String = " "): String = {
        row
          .zip(colWidths)
          .map({ case (s, width) => s.padTo(width, padWith).mkString("") })
          .mkString("")
      }

      dataWithEmptyCells match {
        case header :: rows if hasHeader =>
          (List(row(header.map(_.toUpperCase))) ++ rows.map(row(_))).mkString("\n")
        case rows =>
          rows.map(row(_)).mkString("\n")
      }
    }

  }

}

object StringCaseConvertersTestApp extends App {

  // TODO mw: Write/ Move to Spec

  import StringConverters._

  val someString = ":: Hello! 99World! ::"

  val camel = "helloWorld"

  val kebab = "hello-world"

  val snake = "hello_world"

  println(someString.toCamelCase)
  println(camel.toCamelCase)
  println(kebab.toCamelCase)
  println(snake.toCamelCase)
  println()

  println(someString.toCapitalizedCamelCase)
  println(camel.toCapitalizedCamelCase)
  println(kebab.toCapitalizedCamelCase)
  println(snake.toCapitalizedCamelCase)
  println()

  println(someString.toKebabCase)
  println(camel.toKebabCase)
  println(kebab.toKebabCase)
  println(snake.toKebabCase)
  println()

  println(someString.toSnakeCase)
  println(camel.toSnakeCase)
  println(kebab.toSnakeCase)
  println(snake.toSnakeCase)
  println()
}
