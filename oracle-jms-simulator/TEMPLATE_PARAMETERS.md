# Dynamic Template Parameters - Feature Documentation

## Overview

The Oracle JMS Simulator supports dynamic template parameters in XML messages. This allows you to generate unique values for each message sent, perfect for load testing and realistic data simulation.

## Usage

Simply use `${PLACEHOLDER}` syntax in your XML template. Each message sent will have unique values substituted for the placeholders.

## Visual Indicator

When template variables are detected in your XML:
- An orange info label appears: **"ℹ Dynamic parameters detected - each message will have unique values"**
- This confirms the template will be processed

## Supported Placeholders

### Basic Placeholders

| Placeholder | Type | Description | Example Output |
|------------|------|-------------|----------------|
| `${ID}` | Number | Numeric ID (1-999999999) | `12345678` |
| `${UUID}` | String | Full UUID | `550e8400-e29b-41d4-a716-446655440000` |
| `${NUMBER}` | Number | Random number (1-999999) | `42873` |
| `${RANDOM}` | String | Random 8-character string | `a3f7b2c9` |

### Financial Placeholders

| Placeholder | Type | Description | Example Output |
|------------|------|-------------|----------------|
| `${AMOUNT}` | Decimal | Random amount (0.01-9999.99) | `1234.56` |
| `${PRICE}` | Decimal | Same as AMOUNT | `567.89` |

### Personal Information

| Placeholder | Type | Description | Example Output |
|------------|------|-------------|----------------|
| `${NAME}` | String | Full name | `John Smith` |
| `${FIRSTNAME}` | String | First name only | `Mary` |
| `${FIRST_NAME}` | String | First name only | `David` |
| `${LASTNAME}` | String | Last name only | `Johnson` |
| `${LAST_NAME}` | String | Last name only | `Williams` |
| `${EMAIL}` | String | Email address | `john.smith@example.com` |
| `${PHONE}` | String | Phone number (###-###-####) | `555-123-4567` |

### Date/Time Placeholders

| Placeholder | Type | Description | Example Output |
|------------|------|-------------|----------------|
| `${DATE}` | String | Current date (ISO 8601) | `2023-12-23` |
| `${TIME}` | String | Current time | `12:30:45.123` |
| `${TIMESTAMP}` | String | Current date and time | `2023-12-23T12:30:45.123` |

### Custom Format Placeholders

#### Number Range
Generate a number within a specific range:
```
${NUMBER:min-max}
```

Examples:
- `${NUMBER:1-100}` → `42`
- `${NUMBER:1000-9999}` → `5432`
- `${NUMBER:1-10}` → `7`

#### Amount Range
Generate a decimal amount within a specific range:
```
${AMOUNT:min-max}
```

Examples:
- `${AMOUNT:10-1000}` → `234.56`
- `${AMOUNT:0.01-1.00}` → `0.47`
- `${AMOUNT:50-500}` → `178.23`

#### Random String Length
Generate a random alphanumeric string of specific length:
```
${STRING:length}
```

Examples:
- `${STRING:5}` → `aB3xY`
- `${STRING:10}` → `kL9mP2qR8t`
- `${STRING:3}` → `X7n`

## Complete Example

### Template XML
```xml
<?xml version="1.0" encoding="UTF-8"?>
<order>
    <!-- Unique identifiers -->
    <orderId>${ID}</orderId>
    <orderNumber>ORD-${NUMBER}</orderNumber>
    <uuid>${UUID}</uuid>
    
    <!-- Customer information -->
    <customer>
        <customerId>${UUID}</customerId>
        <firstName>${FIRSTNAME}</firstName>
        <lastName>${LASTNAME}</lastName>
        <fullName>${NAME}</fullName>
        <email>${EMAIL}</email>
        <phone>${PHONE}</phone>
    </customer>
    
    <!-- Order items with custom ranges -->
    <items>
        <item>
            <productId>${NUMBER:1000-9999}</productId>
            <productName>Sample Product</productName>
            <quantity>${NUMBER:1-10}</quantity>
            <unitPrice>${AMOUNT:10-1000}</unitPrice>
            <subtotal>${AMOUNT}</subtotal>
        </item>
        <item>
            <productId>${NUMBER:1000-9999}</productId>
            <productName>Another Product</productName>
            <quantity>${NUMBER:1-10}</quantity>
            <unitPrice>${AMOUNT:10-1000}</unitPrice>
            <subtotal>${AMOUNT}</subtotal>
        </item>
    </items>
    
    <!-- Financial details -->
    <pricing>
        <subtotal>${AMOUNT:100-1000}</subtotal>
        <tax>${AMOUNT:10-100}</tax>
        <shipping>${AMOUNT:5-50}</shipping>
        <total>${AMOUNT:115-1150}</total>
    </pricing>
    
    <!-- Timestamps -->
    <orderDate>${DATE}</orderDate>
    <orderTime>${TIME}</orderTime>
    <createdAt>${TIMESTAMP}</createdAt>
    
    <!-- Additional fields -->
    <status>pending</status>
    <reference>${RANDOM}</reference>
    <trackingCode>${STRING:12}</trackingCode>
</order>
```

### Output Examples

When sending 3 messages with the above template, you might get:

#### Message 1:
```xml
<order>
    <orderId>123456789</orderId>
    <orderNumber>ORD-42873</orderNumber>
    <customer>
        <fullName>John Smith</fullName>
        <email>john.smith@example.com</email>
    </customer>
    <quantity>5</quantity>
    <unitPrice>234.56</unitPrice>
    <orderDate>2023-12-23</orderDate>
    <timestamp>2023-12-23T12:30:45.123</timestamp>
</order>
```

#### Message 2:
```xml
<order>
    <orderId>987654321</orderId>
    <orderNumber>ORD-18945</orderNumber>
    <customer>
        <fullName>Mary Johnson</fullName>
        <email>mary.johnson@test.com</email>
    </customer>
    <quantity>3</quantity>
    <unitPrice>567.89</unitPrice>
    <orderDate>2023-12-23</orderDate>
    <timestamp>2023-12-23T12:30:46.456</timestamp>
</order>
```

#### Message 3:
```xml
<order>
    <orderId>456789123</orderId>
    <orderNumber>ORD-73621</orderNumber>
    <customer>
        <fullName>David Williams</fullName>
        <email>david.williams@demo.com</email>
    </customer>
    <quantity>8</quantity>
    <unitPrice>891.23</unitPrice>
    <orderDate>2023-12-23</orderDate>
    <timestamp>2023-12-23T12:30:47.789</timestamp>
</order>
```

## Batch Sending

### Message Count Feature

Set the number of messages to send using the "Message Count" spinner (1-20000):
- **Single message (count = 1)**: Sends immediately
- **Multiple messages (count > 1)**: Uses multi-threaded batch sending for high performance

### Performance

The simulator automatically optimizes thread usage based on message count:
- **1-99 messages**: Single thread
- **100-999 messages**: Up to 5 threads
- **1000+ messages**: Up to 10 threads

### Progress Tracking

During batch sending:
- Status bar shows real-time progress: "Sending messages: 450/1000 (45.0%)"
- Updates every 10 messages
- Final summary shows total sent and any errors

## Use Cases

### Load Testing
Send 10,000 messages with unique IDs and realistic data:
```xml
<transaction>
    <id>${UUID}</id>
    <amount>${AMOUNT:1-10000}</amount>
    <timestamp>${TIMESTAMP}</timestamp>
</transaction>
```

### User Registration Testing
Simulate user registrations with varied data:
```xml
<user>
    <userId>${ID}</userId>
    <username>${FIRSTNAME}${NUMBER:100-999}</username>
    <email>${EMAIL}</email>
    <registeredAt>${TIMESTAMP}</registeredAt>
</user>
```

### E-commerce Order Simulation
Create realistic order data:
```xml
<order>
    <orderId>ORD-${NUMBER:100000-999999}</orderId>
    <customer>
        <name>${NAME}</name>
        <email>${EMAIL}</email>
    </customer>
    <total>${AMOUNT:50-5000}</total>
    <items>${NUMBER:1-20}</items>
</order>
```

## Technical Details

### Thread Safety
- Each message in a batch gets independent random values
- Thread-safe random number generation
- No conflicts in multi-threaded sending

### Performance
- Fast regex-based placeholder detection
- Minimal overhead per message
- Efficient value generation

### Fallback Behavior
- Unknown placeholders: Returned as-is (e.g., `${UNKNOWN}` → `${UNKNOWN}`)
- Invalid format: Returned as-is (e.g., `${NUMBER:abc}` → `${NUMBER:abc}`)
- No placeholders: XML sent exactly as entered

## Implementation

### Classes
- **TemplateUtil.java**: Core template processing logic
  - Pattern matching for placeholder detection
  - Value generation for all placeholder types
  - Custom range parsing and validation

- **OracleJmsService.sendMessagesAsync()**: Batch sending with threading
  - Thread pool management
  - Progress callbacks
  - Error handling and reporting

- **MainStage.java**: UI components
  - Message count spinner (1-20000)
  - Template indicator label
  - Progress updates in status bar
