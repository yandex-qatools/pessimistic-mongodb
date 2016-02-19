# Pessimistic MongoDB
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ru.qatools/pessimistic-mongodb/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.qatools/pessimistic-mongodb) 
[![Build status](http://ci.qatools.ru/job/pessimistic-mongodb_master-deploy/badge/icon)](http://ci.qatools.ru/job/pessimistic-mongodb_master-deploy/) [![covarage](https://img.shields.io/sonar/http/sonar.qatools.ru/ru.qatools:pessimistic-mongodb/coverage.svg?style=flat)](http://sonar.qatools.ru/dashboard/index/2302)

Not a very new, and, probably completely useless approach to implement the distributed pessimistic locking on top of MongoDB.
Allows to use MongoDB as a key-value storage with the ability to read-write using pessimistic locks.
Additionally this library contains the simple queue implementation that uses tailing cursor.

### Maven

Add the following dependency to your pom.xml:
```xml
    <dependency>
        <groupId>ru.qatools</groupId>
        <artifactId>pessimistic-mongodb</artifactId>
        <version>1.9</version>
    </dependency>
```

### Usage
To use locking as a named locks (by key):
```java
    final PessimisticLockng locking = new MongoPessimisticLocking(
          mongoClient,          // an instance of MongoClient
          "locks-database",     // mongo database name
          "locks-collection",   // collection to use
          30                    // a maximum interval of polling the locks (ms)
    );
    locking.tryLock("some-key", 5000); // trying to lock the key "some-key" with 5s timeout
    new Thread(() -> locking.tryLock("some-key", 5000) ).start(); // thread will wait for 5s 
    locking.unlock("some-key");  // after this the thread will obtain the lock forever
```

To use a pessimistic repository:
```java
    final PessimisticRepository<String> repo = new MongoPessimisticRepository<String>( locking, String.class );
    String value = repo.tryLockAndGet("key", 3000); // locking the 'key'
    repo.putAndUnlock("key", "new value"); // releasing the 'key' and writing 'new value' to the repository
    repo.removeAndUnlock("key"); // removing key from the repository
```

To obtain a new unique lock (implementing java.util.concurrent.Lock):
```java
  final Lock lock = new MongoPessimisticLock( locking )
  lock.tryLock(5, SECONDS); // lock the unique lock
  lock.unlock();
```

To use the simple queue polling
```java
  final TailingQueue<String> queue = new MongoTailingQueue( 
        mongoClient,        // instance of MongoClient
        "databaseName",     // mongo db name
        "collectionName",   // collection name to use (must be capped!)
        10000               // max messages count within queue
  );
  new Thread(() -> queue.poll( m -> System.out.println(m))).start(); // launch polling thread
  queue.add( "hello" ); // send some messages to the queue (will be displayed on console)
```
