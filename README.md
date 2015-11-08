# Pessimistic MongoDB

Not a very new, and, probably completely useless approach to implement the distributed pessimistic locking on top of MongoDB.
Allows to use MongoDB as a key-value storage with the ability to read-write using pessimistic locks.

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
    final PessimisticRepository repo = new MongoPessimisticRepository( locking );
    Object value = repo.tryLockAndGet("key", 3000); // locking the 'key'
    repo.putAndUnlock("key", "new value"); // releasing the 'key' and writing 'new value' to the repository
    repo.removeAndUnlock("key"); // removing key from the repository
```

To obtain a new unique lock (implementing java.util.concurrent.Lock):
```java
  final Lock lock = new MongoPessimisticLock( locking )
  lock.tryLock(5, SECONDS); // lock the unique lock
  lock.unlock();
```

