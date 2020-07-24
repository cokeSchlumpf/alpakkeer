package alpakkeer.scaladsl

import _root_.{ alpakkeer => j }

class Alpakkeer(d: j.Alpakkeer) {

  def toJava: j.Alpakkeer = d

}

object Alpakkeer {

  class AlpakkeerBuilder(d: j.AlpakkeerBuilder) {

    def start(): Alpakkeer = {
      Alpakkeer(d.start())
    }

    def toJava: j.AlpakkeerBuilder = d

  }

  private def apply(d: j.Alpakkeer): Alpakkeer = {
    new Alpakkeer(d)
  }

  def create(): AlpakkeerBuilder = {
    new AlpakkeerBuilder(j.Alpakkeer.create())
  }

}
