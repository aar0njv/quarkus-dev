# Context and Dependency Injection
Quarkus uses ArC, a build-time implementation of Contexts and Dependency Injection (CDI). Instead of scanning your classes at runtime (which is slow and uses a lot of memory), Quarkus figures out how everything connects while your code is compiling## Beans, Scopes and Injections

## Beans, Scopes and Injection
In CDI, a Bean is simply a Java class object managed by the framework. You control how and when these beans are created using scope annotations.

The container `creates the object->manages it->inject it to other classes->destroy when scope ends`

- `@ApplicationScoped` - default choice, creates one contextual bean instance for the application. *The instance is shared across different requests throughout the application*. Bean is created lazily, when it is first accessed by another class and remains available until the application shuts down

- `@Singleton` - creates only one instance of the bean and is shared across the appl. unlike `ApplicationScoped`, doesnt use a CDI client proxy. It is created when the bean is first needed

- `@RequestScoped` - creates a bean instance for each HTTP request (or CD request context). The instance is available only during that request. Different requests get different instances. Bean is destroyed when request ends. (used for storing temporary info related to particular user's HTTP request)

- `@Startup` (*doesnt represent a bean scope*) - used to initialize a bean when the application starts. tells Quarkus to instantiate the bean during application startup. Commonly used with `ApplicationScoped` 