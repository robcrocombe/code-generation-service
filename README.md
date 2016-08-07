# Code Generation as a Service

Model-driven engineering (MDE) is a software engineering paradigm that focuses on modelling systems during development with the effect of increasing thoughtful design and reducing accidental complexity. Models help design software before it is implemented, representing systems as common data structures.

MDE tools, such as model-to-text transformers can support the transition from working with models to working with code. Text generated from a model can act as a base to develop on, present statistics, or provide documentation, for example.

This project makes code generation a web service. This has a variety of advantages over traditional methods, such as:

- Having the ability to generate code from models without the need for locally-available software.
- Removes the effort of learning and maintaining the generator software from the user. It benefits the situation where the individual writing the code generator is different to the one designing the models, like a team or client. The model designer does not need to know how the generator works but can benefit from the output it creates using their own models.
- The internal system can change, to be optimized or made faster for example, but the API stays the same.
- Benefits very large models depicting a complex system, as computing in the cloud can have increased processing power and resources over a local machine.

## How it works

The web service allows users to publish [Epsilon](http://www.eclipse.org/epsilon/) code generators and execute them online. Generators are publicly available so anyone can use them on their own models. A user uploads their models to be processed and receives the output files produced by their chosen generator.

Here's an example, converting XML to a Java class. I publish the generator `cgs-java-gen` (which you can find on [GitHub](https://github.com/robcrocombe/cgs-java-gen/)) to the service. Using the CLI client it's as easy as:

```
> codegen publish robcrocombe/cgs-java-gen
```

XML is a language that can represent a model. Here is an example model of a simple `Person` class:

```xml
<class name="Person" access="public">
  <property name="name" type="String" access="public"></property>
  <property name="age" type="int" access="public"></property>
  <property name="address" type="String" access="private"></property>
</class>
```

The model can be sent to the service to be generated into a Java class. Here is how it would be done with the CLI client:

```
> codegen run robcrocombe/cgs-java-gen model="person.xml"
```

The `model` parameter specifies the XML file to use. Once the generator completes, it returns a Zip of our output - the `person.java` file:

```java
public class Person {
  public String name;
  public int age;
  private String address;

  public String getAddress() {
    return address;
  }
}
```

You can see the generator has made a basic starting point for adding to this class, including a public getter for the private `address` variable I set.

This is a very basic example, but the Code Generation Service can handle any generator created for the [Epsilon](http://www.eclipse.org/epsilon/) MDE framework. You can see more examples on my [GitHub](https://github.com/search?q=user%3Arobcrocombe+cgs) that transform EMF models to HTML pages.

## Future Work

This project was developed for a masters of Computer Science dissertation under a strict deadline and should be treated as a working prototype. There may be issues with the way the network code was written as it was my first time developing a RESTful API. There are definitely security issues as there is nothing to stop generators running any code via Ant. Also, Job IDs are sequential, so it would be trivial to access other users' running jobs.

Generator publishing/usage doesn't support private GitHub repositories yet. This would be a nice improvement to prevent anyone from using a generator or re-publishing it.
