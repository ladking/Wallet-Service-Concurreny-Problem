# Decison Documentation 

### Why the original code failed under concurrency
The original code failed under concurrency due to race conditions. When multiple requests to process payment on a user wallet are recieved concurrently, each requests runs the same database operations on the user wallet. Because the requests run concurrently, they would read the same wallet balance and carry out business logic using a stale balance, this could lead to overspending, lost updates or negative wallet balance in the case of insufficient funds. 

### Why you choose your specific locking/concurrency mechanism
To prevent this race conditons, I made use of database locking to handle the concurrency. The locking mechanism i used is **Pessimistic Locking** which allows a row-level lock to be aquired at a time, and until the transaction holding the lock commits or rollback, other transactions that is attempting to aquire a lock on the same row will wait.

#### Why Pessimistic Locking Over Optimistic Lock
Pessimistic locking allows the row operated on to be locked hence blocking other transactions attempting to aquire lock on the same row until the current transaction is completed and the changes are commited / rolled back, this will ensure that every operations is working with the most current and updated row record rather than proceeding using a stale balance, this will also ensure transactions on a wallet are serialized.

Optimistic lock  on the other hand will also prevent the concurrency bug, concurrent transactions will be able to proceed without waiting for previous transactions to be completed, but the transaction will fail on update when conflict with the version of the row that was read is detected.

The Trade off for both approach is that for Pessimistic locking, each request will block until the previous transaction is completed while for optimistc locking transactions will not be blocked but rather only one will succeed while the others will fail on conflict.

### Idempotency Implementation
I added an Idempotency key to the processPayment which is used to ensure that transaction is processed at most once for identical requests, This ensures that duplicate requests are not processed but rather return the result of the first transactions preventing double-spend.

