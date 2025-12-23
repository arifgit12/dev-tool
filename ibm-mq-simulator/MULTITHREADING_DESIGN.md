# Multi-Threaded Message Sending - UI Design

## Updated Send Panel Layout

```
┌────────────────────────────────────────────────────────────┐
│ Send Message                                               │
│ ────────────────────────────────────────────────────────── │
│                                                            │
│ Queue: [DEV.QUEUE.2            ▼]                         │
│                                                            │
│ Message Count: [     1    ▲▼]    Threads: [   1  ▲▼]     │
│                (1-20,000)                  (1-100)         │
│                                                            │
│ XML Message Content:                                       │
│ ┌────────────────────────────────────────────────────────┐ │
│ │<?xml version="1.0" encoding="UTF-8"?>                 │ │
│ │<order>                                                 │ │
│ │    <orderId>ORD-123</orderId>                         │ │
│ │    <customer>                                          │ │
│ │        <name>John Doe</name>                          │ │
│ │    </customer>                                         │ │
│ │    <items>                                             │ │
│ │        <item>                                          │ │
│ │            <name>Product</name>                        │ │
│ │            <price>99.99</price>                        │ │
│ │        </item>                                         │ │
│ │    </items>                                            │ │
│ │</order>                                                │ │
│ │                                                        │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                            │
│ ✓ Valid XML                                               │
│                                                            │
│ Sending 5000/10000 messages...                            │
│ [████████████████████░░░░░░░░░░░░░░░░░░░░] 50%           │
│                                                            │
│ [Beautify XML] [Send Message] [Clear]                     │
│    (Blue)         (Green)      (Gray)                      │
│                                                            │
│ Message History:                                           │
│ ┌────────────────────────────────────────────────────────┐ │
│ │→ 2023-12-23 12:28:45 [DEV.QUEUE.2] SENT              │ │
│ │→ 2023-12-23 12:29:12 [Batch 10/10000] [DEV.QUEUE.2]  │ │
│ │→ 2023-12-23 12:29:15 [Batch 20/10000] [DEV.QUEUE.2]  │ │
│ └────────────────────────────────────────────────────────┘ │
│ [Clear History]                                            │
└────────────────────────────────────────────────────────────┘
```

## Feature Specifications

### Message Count Spinner
- **Label**: "Message Count:"
- **Control Type**: Spinner (numeric input with up/down arrows)
- **Range**: 1 to 20,000
- **Default Value**: 1
- **Editable**: Yes (can type directly)
- **Width**: 100px
- **Styling**: Dark background (#3e3e3e)

### Thread Count Spinner
- **Label**: "Threads:"
- **Control Type**: Spinner (numeric input with up/down arrows)
- **Range**: 1 to 100
- **Default Value**: 1
- **Editable**: Yes (can type directly)
- **Width**: 80px
- **Styling**: Dark background (#3e3e3e)

### Progress Bar
- **Location**: Between validation label and action buttons
- **Width**: 400px
- **Visibility**: Hidden by default, shown during batch sending
- **Color**: Green accent (#4CAF50)
- **Updates**: Real-time during message sending

### Progress Label
- **Location**: Above progress bar
- **Format**: "Sending X/Y messages..."
- **Color**: Medium gray (#b0b0b0)
- **Font Size**: 11px
- **Visibility**: Hidden by default, shown during batch sending

## UI Behavior

### Idle State
```
Message Count: [1] Threads: [1]
Progress: (hidden)
Buttons: [Beautify] [Send] [Clear] - all enabled (if connected)
```

### During Batch Send
```
Message Count: [1000] - DISABLED
Threads: [10] - DISABLED
Progress: Sending 450/1000 messages...
         [███████████████████░░░░░░░░░] 45%
Buttons: [Beautify] [Send] [Clear]
         enabled   DISABLED enabled
```

### After Completion
```
Message Count: [1000] - ENABLED
Threads: [10] - ENABLED
Progress: (hidden)
Status Bar: "Successfully sent 1000 messages to DEV.QUEUE.2 using 10 threads"
Buttons: All restored to original state
```

## Technical Implementation

### Multi-Threading Architecture

```
Main Thread (UI)
    │
    └─> Spawns Background Thread
            │
            ├─> Creates ExecutorService with N threads
            │   │
            │   ├─> Thread 1: Session 1 → Producer 1 → Send messages
            │   ├─> Thread 2: Session 2 → Producer 2 → Send messages
            │   ├─> Thread 3: Session 3 → Producer 3 → Send messages
            │   └─> ... (up to N threads)
            │
            └─> Progress Callback → Update UI
                    │
                    └─> Platform.runLater() → Update progress bar
```

### Thread Safety
- Each thread creates its own JMS Session and MessageProducer
- AtomicInteger for thread-safe counter updates
- CountDownLatch ensures all messages sent before completion
- ExecutorService manages thread pool lifecycle

### Performance Optimizations
1. **Sampled History Updates**: Only add to history every 10 messages
2. **Atomic Operations**: Thread-safe counters without locks
3. **Thread Pool**: Reuse threads efficiently
4. **Session Per Thread**: Avoid contention on shared resources

## Example Use Cases

### Use Case 1: Load Testing
```
Scenario: Test queue capacity
Message Count: 10,000
Threads: 50
Expected Time: ~20 seconds (500 msg/sec)
```

### Use Case 2: Single Message
```
Scenario: Send one formatted XML
Message Count: 1
Threads: 1
Expected Time: <1 second
```

### Use Case 3: Moderate Batch
```
Scenario: Send batch of 1000 messages
Message Count: 1,000
Threads: 10
Expected Time: ~10 seconds (100 msg/sec)
```

### Use Case 4: Maximum Performance
```
Scenario: Maximum throughput test
Message Count: 20,000
Threads: 100
Expected Time: ~40 seconds (500 msg/sec)
```

## Status Messages

### Success
```
"Successfully sent 1000 messages to DEV.QUEUE.2 using 10 threads"
Color: Green (#4CAF50)
```

### Error
```
"Failed to send 50 messages out of 1000"
Color: Red (#f44336)
Alert dialog shows detailed error
```

### Progress Updates (Real-time)
```
"Sending 0/1000 messages..."
"Sending 100/1000 messages..."
"Sending 500/1000 messages..."
"Sending 1000/1000 messages..."
```

## History Format

### Single Messages
```
→ 2023-12-23 12:30:45 [DEV.QUEUE.2] SENT
```

### Batch Messages (Sampled)
```
→ 2023-12-23 12:30:50 [Batch 10/1000] <?xml version...
→ 2023-12-23 12:30:51 [Batch 20/1000] <?xml version...
→ 2023-12-23 12:30:52 [Batch 30/1000] <?xml version...
```

This implementation provides:
- ✅ Configurable message count (1-20,000)
- ✅ Configurable thread count (1-100)
- ✅ Real-time progress tracking
- ✅ Professional UI integration
- ✅ High-performance multi-threaded delivery
- ✅ Thread-safe operations
- ✅ Proper error handling
