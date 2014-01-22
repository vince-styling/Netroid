

Before build this project, please confirm your Maven version is `3.1.1` or later :

    $ mvn --version :

    Apache Maven 3.1.1
    Maven home: /usr/share/maven
    Java version: 1.6.0_43, vendor: Apple Inc.
    Java home: /System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home
    Default locale: en_US, platform encoding: MacRoman
    OS name: "mac os x", version: "10.8.2", arch: "x86_64", family: "mac"



If you want just pack the Netroid library as jar file, below commands you could use :

    $ cd ${project.root}/netroid/
    $ mvn clean install

The latest jar file will generate at `target` directory when you build success.



If you want deploy the Netroid-Sample application to the device or emulator, below commands you could use :

    $ cd ${project.root}/netroid-sample/
    $ mvn clean install
    $ mvn android:deploy
    $ mvn android:run



To be import to eclipse IDE, go to project root and run eclipse:eclipse command :

    $ cd ${project.root}
    $ mvn eclipse:clean
    $ mvn eclipse:eclipse

for Intellij IDEA, just **import project** with root pom.xml.
