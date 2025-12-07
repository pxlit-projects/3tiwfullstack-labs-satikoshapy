API Gateway

Routes:

/post/** → Post service (8081)

/review/** → Review service (8082)

/comment/** → Comment service (8084)

Post Service (8081)
Responsibilities:

Create/edit/view posts

Submit post for review

Update post status based on review decisions

Communication:

Receives async messages from RabbitMQ
→ Updates post status to PUBLISHED or REJECTED

Responds to synchronous OpenFeign calls from:

Review service

Comment service

Review Service (8082)
Responsibilities:

Receive review submission

Approve/reject posts

Store review decisions

Communication:

Sync → Post Service (OpenFeign)
→ getPostById(postId, "internal")

Async → RabbitMQ
→ Sends PostReviewedEvent(postId, decision)

Comment Service (8084)
Responsibilities:

Add/edit/delete comments

Retrieve comments

Communication:

Sync → Post Service (OpenFeign)
→ Ensures the post is visible before showing comments

RabbitMQ (Async Messaging)
Exchange: review.exchange
Routing Key: post.reviewed
Queue: review.decisions

Flow:

Review Service publishes PostReviewedEvent

RabbitMQ routes event into review.decisions

Post Service consumes it via a @RabbitListener

Post is updated to PUBLISHED or REJECTED

Config Server

All services load shared config (YAML) from Git

Reduces duplication and centralizes configuration management
