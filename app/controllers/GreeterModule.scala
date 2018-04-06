package controllers

import io.grpc.examples.GreeterService
import play.api.inject.Binding
import play.api.{Configuration, Environment}

class GreeterModule extends play.api.inject.Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[GreeterService].qualifiedWith("client").to[InjectedGreeterServiceClient],
    bind[GreeterService].qualifiedWith("impl").to[GreeterServiceImpl]
  )
}