package uk.gov.gds.common.mongo.repository

import net.sf.cglib.proxy._
import java.lang.reflect.Method
import org.objenesis.ObjenesisStd
import com.novus.salat.CaseClass
import uk.gov.gds.common.logging.Logging

object ModelProxyFactory extends Logging {

  private[repository] def proxy[A <: CaseClass]()(implicit m: Manifest[A]): A = {
    val proxyType = m.erasure.asInstanceOf[Class[A]]
    proxyType.cast(createProxy(createProxyClass(proxyType)))
  }

  private lazy val objenesis = new ObjenesisStd()

  private lazy val ignoreBridgeMethods = new CallbackFilter() {
    def accept(method: Method) = if (method.isBridge()) 1 else 0
  }

  private object SerializableNoOp extends NoOp with Serializable

  private object DefaultMethodInterceptor extends MethodInterceptor {
    def intercept(p1: Any, p2: Method, p3: Array[AnyRef], p4: MethodProxy): AnyRef = null
  }

  private def createProxyClass[A <: CaseClass](proxyType: Class[A], interfaces: List[Class[_]] = Nil) = {
    val enhancer = new Enhancer

    proxyType.getDeclaredConstructors.filter(_.isAccessible == false).foreach(_.setAccessible(true))

    enhancer.setClassLoader(proxyType.getClassLoader) // TODO: Will this always work in all contexts?
    enhancer.setUseFactory(true)
    enhancer.setSuperclass(proxyType)
    enhancer.setCallbackTypes(List(classOf[MethodInterceptor], classOf[NoOp]).toArray)
    enhancer.setCallbackFilter(ignoreBridgeMethods)

    if (interfaces != Nil)
      enhancer.setInterfaces(interfaces.toArray)

    enhancer.createClass()
  }

  private def createProxy(proxyClass: Class[_]): AnyRef = {
    val proxy = objenesis.newInstance(proxyClass).asInstanceOf[Factory]

    proxy.setCallbacks(List(DefaultMethodInterceptor, SerializableNoOp).toArray)
    proxy
  }
}
