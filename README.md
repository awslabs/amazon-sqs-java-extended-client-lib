Amazon SQS Extended Client Library for Java
===========================================
The **Amazon SQS Extended Client Library for Java** enables you to manage Amazon SQS message payloads with Amazon S3. This is especially useful for storing and retrieving messages with a message payload size greater than the current SQS limit of 256 KB, up to a maximum of 2 GB. Specifically, you can use this library to:

* Specify whether message payloads are always stored in Amazon S3 or only when a message's size exceeds 256 KB.

* Send a message that references a single message object stored in an Amazon S3 bucket.

* Get the corresponding message object from an Amazon S3 bucket.

* Delete the corresponding message object from an Amazon S3 bucket.

You can download release builds through the [releases section of this](https://github.com/awslabs/amazon-sqs-java-extended-client-lib) project.

For more information on using the amazon-sqs-java-extended-client-lib, see our getting started guide [here](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/s3-messages.html).

##Getting Started

* **Sign up for AWS** -- Before you begin, you need an AWS account. For more information about creating an AWS account and retrieving your AWS credentials, see [AWS Account and Credentials](http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-setup.html) in the AWS SDK for Java Developer Guide.
* **Sign up for Amazon SQS** -- Go to the Amazon [SQS console](https://console.aws.amazon.com/sqs/home?region=us-east-1) to sign up for the service.
* **Minimum requirements** -- To use the sample application, you'll need Java 1.7+ and [Maven 3](http://maven.apache.org/). For more information about the requirements, see the [Getting Started](http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/s3-messages.html) section of the Amazon SQS Developer Guide.
* **Download** -- Download the [latest preview release](https://github.com/awslabs/amazon-sqs-java-extended-client-lib/releases) or pick it up from Maven:
```xml
  <dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>amazon-sqs-java-extended-client-lib</artifactId>
    <version>1.0.0</version>
    <type>jar</type>
  </dependency>
```
* **Further information** - Read the [API documentation](http://aws.amazon.com/documentation/sqs/).

##Feedback
* Give us feedback [here](https://github.com/awslabs/amazon-sqs-java-extended-client-lib/issues).
* If you'd like to contribute a new feature or bug fix, we'd love to see Github pull requests from you.
