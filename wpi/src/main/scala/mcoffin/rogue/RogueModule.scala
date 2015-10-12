package mcoffin.rogue

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

import org.osgi.framework.BundleContext
import org.osgi.framework.Constants
import org.osgi.framework.ServiceListener
import org.osgi.framework.ServiceReference
import org.osgi.framework.ServiceEvent

import scala.reflect.Manifest

class RogueModule (val bundleContext: BundleContext) extends AbstractModule with ScalaModule with ServiceListener {
  val boundClasses = scala.collection.mutable.Buffer[String]()

  private[RogueModule] def bindServiceReference(sr: ServiceReference[_]) {
    val srClassNames = sr.getProperty(Constants.OBJECTCLASS).asInstanceOf[Array[String]]
    val impl = bundleContext.getService(sr)
    val srClasses = srClassNames.map(Class.forName(_))
    for (srClass <- srClasses) {
      val srClassName = srClass.getCanonicalName
      if (!boundClasses.contains(srClassName)) {
        boundClasses += srClassName
        srClass match {
          case clazz: Class[Object] => {
            bind(clazz).toInstance(impl.asInstanceOf[Object])
          }
          case _ => throw new Exception("Illegal service registration not of type Object")
        }
      }
    }
  }

  override def configure {
    bundleContext.addServiceListener(this)
    bundleContext.getAllServiceReferences(null, null).foreach(sr => bindServiceReference(sr))
  }

  override def serviceChanged(evt: ServiceEvent) {
    if (evt.getType != ServiceEvent.UNREGISTERING) {
      bindServiceReference(evt.getServiceReference)
    }
  }
}
