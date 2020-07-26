package alpakkeer.scaladsl

import alpakkeer.core.resources.Resources
import alpakkeer.javadsl.{AlpakkeerRuntime, AlpakkeerRuntimeBuilder, Alpakkeer => JAlpakkeer, AlpakkeerBuilder => JAlpakkeerBuilder}
import io.javalin.Javalin

class Alpakkeer(d: JAlpakkeer) {

  def toJava: JAlpakkeer = d

}

object Alpakkeer {

  class AlpakkeerBuilder(d: JAlpakkeerBuilder) {

    /**
     * Use this method to override the defaults of the Alpakkeer runtime configuration.
     *
     * @param configure A function which accepts the configuration builder
     * @return The current builder instance
     */
    def configure(configure: AlpakkeerRuntimeBuilder => Unit): this.type = {
      d.configure(configure(_))
      this
    }

    /**
     * Use this method to extend the API with custom endpoints.
     *
     * @param apiExtension A function which takes the Javalin instance
     * @return The current builder instance
     */
    def withApiEndpoint(apiExtension: Javalin => Unit): this.type = {
      d.withApiEndpoint(apiExtension(_))
      this
    }

    /**
     * Use this method to extend the API with custom endpoints.
     *
     * @param apiExtension A function which takes the Javalin instance and the { @link AlpakkeerRuntime}
     *                                                        of Alpakkeer instance
     * @return The current builder instance
     */
    def withApiEndpoint(apiExtension: (Javalin, AlpakkeerRuntime) => Unit): this.type  = {
      d.withApiEndpoint((_, _))
      this
    }

    /**
     * Use this method to extend the API with custom endpoints.
     *
     * @param apiExtension A function which takes the Javalin instance, the AlpakkeerRuntime and
     *                                                        the Resources of the Alpakkeer instance
     * @return The current builder instance
     */
    def withApiEndpoint(apiExtension: (Javalin, AlpakkeerRuntime, Resources)): this.type  = {
      d.withApiEndpoint((_, _, _))
      this
    }
    
    def withJob[P, C]()


    def start() = Alpakkeer(d.start())

    def toJava = d

  }

  private def apply(d: JAlpakkeer): Alpakkeer = {
    new Alpakkeer(d)
  }

  /**
   * Use this method start the definition of an Alpakkeer application.
   *
   * @return The Alpakkeer builder DSL
   */
  def apply(): AlpakkeerBuilder = create()

  /**
   * Use this method start the definition of an Alpakkeer application.
   *
   * @return The Alpakkeer builder DSL
   */
  def create(): AlpakkeerBuilder = {
    new AlpakkeerBuilder(JAlpakkeer.create())
  }

}
