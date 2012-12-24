package uk.gov.gds.common.mongo.repository

import net.sf.cglib.proxy._
import java.lang.reflect.Method
import org.objenesis.ObjenesisStd
import com.novus.salat.CaseClass
import uk.gov.gds.common.logging.Logging
import com.mongodb.casbah.commons.MongoDBObject

object ModelProxyFactory extends Logging {

  private[repository] def proxy[A <: CaseClass](implicit m: Manifest[A]): A = new ProxyFactory[A].proxy

  private lazy val objenesis = new ObjenesisStd()

  private object ignoreBridgeMethods extends CallbackFilter {
    def accept(method: Method) = if (method.isBridge) 1 else 0
  }

  private object SerializableNoOp extends NoOp with Serializable

  private class ProxyFactory[A <: CaseClass](implicit m: Manifest[A]) {

    private val proxyType = m.erasure.asInstanceOf[Class[A]]
    private lazy val queryBuilder = MongoDBObject()

    lazy val proxy: A = {
      val proxyClass = {
        val enhancer = new Enhancer

        proxyType.getDeclaredConstructors.find(_.isAccessible == false).foreach(_.setAccessible(true))
        enhancer.setClassLoader(proxyType.getClassLoader)
        enhancer.setUseFactory(true)
        enhancer.setSuperclass(proxyType)
        enhancer.setCallbackTypes(Array(classOf[MethodInterceptor], classOf[NoOp]))
        enhancer.setCallbackFilter(ignoreBridgeMethods)
        enhancer.createClass
      }

      val proxy = objenesis.newInstance(proxyClass)
      proxy.asInstanceOf[Factory].setCallbacks(Array(DefaultMethodInterceptor, SerializableNoOp))
      proxyType.cast(proxy)
    }

    private object DefaultMethodInterceptor extends MethodInterceptor {

      def intercept(obj: Any, method: Method, args: Array[AnyRef], proxy: MethodProxy): AnyRef = {

        null
      }
    }

  }

}
