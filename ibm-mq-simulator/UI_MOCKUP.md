# IBM MQ Simulator - UI Screenshots & Mockup

## Application Window

Since we cannot run the application without an IBM MQ server, here's a detailed description of what the UI looks like:

### Main Window (1400x900 pixels)

```
╔══════════════════════════════════════════════════════════════════════════════╗
║ IBM MQ Simulator - Professional Edition                              [_][□][X]║
╠══════════════════════════════════════════════════════════════════════════════╣
║  ┌────────────────────────────────────────────────────────────────────────┐ ║
║  │ IBM MQ Connection                                                      │ ║
║  │ ────────────────────────────────────────────────────────────────────── │ ║
║  │  Queue Manager:  QM1                    Channel:  DEV.APP.SVRCONN      │ ║
║  │  Connection:     localhost(1414)        User:     app                  │ ║
║  │                                                                         │ ║
║  │  [  Connect  ]  [ Disconnect ]                                         │ ║
║  └────────────────────────────────────────────────────────────────────────┘ ║
╠══════════════════════════════════════════════════════════════════════════════╣
║  ┌────────────────────────────────┬───────────────────────────────────────┐ ║
║  │ Send Message                   │ Receive Messages                      │ ║
║  │ ────────────────────────────── │ ───────────────────────────────────── │ ║
║  │                                │                                       │ ║
║  │ Queue: [DEV.QUEUE.2      ▼]    │ Queue: [DEV.QUEUE.1      ▼]          │ ║
║  │                                │                                       │ ║
║  │ XML Message Content:           │ ┌───────────────────────────────────┐ │ ║
║  │ ┌────────────────────────────┐ │ │ ============================      │ │ ║
║  │ │<?xml version="1.0"?>       │ │ │ Time: 2023-12-23 12:30:15        │ │ ║
║  │ │<order>                     │ │ │ Queue: DEV.QUEUE.1               │ │ ║
║  │ │    <orderId>ORD-123</orderId>│ │ Message ID: ID:414d51204...      │ │ ║
║  │ │    <customer>              │ │ │ ----------------------------      │ │ ║
║  │ │        <name>John Doe</name>│ │ │ <?xml version="1.0"?>            │ │ ║
║  │ │    </customer>             │ │ │ <response>                        │ │ ║
║  │ │    <total>999.99</total>   │ │ │     <status>success</status>      │ │ ║
║  │ │</order>                    │ │ │ </response>                       │ │ ║
║  │ │                            │ │ │ ============================      │ │ ║
║  │ │                            │ │ │                                   │ │ ║
║  │ │                            │ │ │                                   │ │ ║
║  │ │                            │ │ │                                   │ │ ║
║  │ └────────────────────────────┘ │ └───────────────────────────────────┘ │ ║
║  │                                │                                       │ ║
║  │ ✓ Valid XML                    │                                       │ ║
║  │                                │                                       │ ║
║  │ [Beautify XML][Send Message][Clear]  [Receive Messages][Clear]        │ ║
║  │    (Blue)        (Green)   (Gray)       (Orange)       (Gray)         │ ║
║  │                                │                                       │ ║
║  │ Message History:               │                                       │ ║
║  │ ┌────────────────────────────┐ │                                       │ ║
║  │ │→ 2023-12-23 12:28:45 [...] │ │                                       │ ║
║  │ │→ 2023-12-23 12:29:12 [...] │ │                                       │ ║
║  │ │← 2023-12-23 12:30:15 [...] │ │                                       │ ║
║  │ └────────────────────────────┘ │                                       │ ║
║  │ [Clear History]                │                                       │ ║
║  └────────────────────────────────┴───────────────────────────────────────┘ ║
╠══════════════════════════════════════════════════════════════════════════════╣
║ ● Connected to QM1                                                           ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

## Color Scheme

### Background Colors
- **Main Window**: #2b2b2b (Dark gray)
- **Panels**: #1e1e1e (Darker gray for contrast)
- **Text Areas**: #1e1e1e with monospace font
- **Borders**: #3e3e3e (Medium gray)

### Text Colors
- **Primary Text**: #e0e0e0 (Light gray)
- **Secondary Text**: #b0b0b0 (Medium gray)
- **Success Text**: #4CAF50 (Green)
- **Error Text**: #f44336 (Red)
- **Warning Text**: #FF9800 (Orange)

### Button Colors
- **Connect**: #4CAF50 (Green) with hover effect
- **Disconnect**: #f44336 (Red) with hover effect
- **Beautify XML**: #2196F3 (Blue) with hover effect
- **Send Message**: #4CAF50 (Green) with hover effect
- **Receive Messages**: #FF9800 (Orange) with hover effect
- **Clear/Neutral**: #757575 (Gray) with hover effect

### Status Indicators
- **Connected**: Green dot with green text
- **Disconnected**: Red text
- **Valid XML**: ✓ with green text
- **Invalid XML**: ✗ with red text and error message

## UI States

### 1. Initial State (Disconnected)
```
Connection Panel:
  - Connection details visible (read-only)
  - [Connect] button: ENABLED (green)
  - [Disconnect] button: DISABLED (gray)

Send Panel:
  - XML input: EMPTY
  - Validation: (hidden)
  - [Beautify XML]: ENABLED
  - [Send Message]: DISABLED (gray)
  - History: EMPTY

Receive Panel:
  - Messages area: EMPTY
  - [Receive Messages]: DISABLED (gray)

Status Bar:
  - Text: "Disconnected" (red)
```

### 2. Connected State
```
Connection Panel:
  - [Connect] button: DISABLED (gray)
  - [Disconnect] button: ENABLED (red)

Send Panel:
  - [Send Message]: ENABLED IF XML is valid (green)
  - [Send Message]: DISABLED IF XML is invalid (gray)

Receive Panel:
  - [Receive Messages]: ENABLED (orange)

Status Bar:
  - Text: "Connected to QM1" (green)
```

### 3. Valid XML Entered
```
Send Panel:
  - XML input: Contains valid XML
  - Validation: "✓ Valid XML" (green)
  - [Send Message]: ENABLED (green) - IF CONNECTED
```

### 4. Invalid XML Entered
```
Send Panel:
  - XML input: Contains invalid XML
  - Validation: "✗ XML Parse Error: ..." (red)
  - [Send Message]: DISABLED (gray)
```

### 5. Message Sent
```
Status Bar:
  - Text: "Message sent successfully to DEV.QUEUE.2" (green)

Message History:
  - New entry: "→ 2023-12-23 12:30:45 [DEV.QUEUE.2] SENT"
```

### 6. Messages Received
```
Status Bar:
  - Text: "Received 3 message(s) from DEV.QUEUE.1" (green)

Receive Panel:
  - Messages displayed with formatting
  - Each message includes timestamp, queue, ID, and content

Message History:
  - New entries: "← 2023-12-23 12:31:00 [DEV.QUEUE.1] RECEIVED"
```

## Interactive Elements

### Buttons
All buttons have:
- Rounded corners (4px radius)
- Bold white text
- Padding: 8px horizontal, 16px vertical
- Hover effect: Slightly darker shade (-10%)
- Hand cursor on hover
- Smooth transitions

### Text Areas
- Monospace font (Courier New, 12px)
- Dark background with light text
- Scroll bars when content exceeds area
- Automatic word wrapping for send panel
- Read-only for receive panel

### Dropdowns (ComboBox)
- Dark background (#3e3e3e)
- Light text (#e0e0e0)
- 200px width
- Shows queue names from configuration

### Split Pane
- Vertical divider between send/receive panels
- 50/50 split by default
- Resizable by dragging divider
- Divider color: #3e3e3e

## Accessibility Features

- High contrast colors for readability
- Clear visual feedback for all actions
- Disabled states clearly indicated
- Error messages displayed prominently
- Status updates in dedicated status bar
- Large clickable areas for buttons
- Keyboard navigation support for text areas

## Professional Touches

1. **Consistent Spacing**: 10-15px padding throughout
2. **Visual Hierarchy**: Clear sections with headers
3. **Color Coding**: Actions color-coded by purpose
4. **Real-time Feedback**: Instant validation updates
5. **Loading States**: Status messages during operations
6. **Clean Layout**: Split-pane for balanced workspace
7. **Professional Typography**: System fonts with proper sizing
8. **Dark Theme**: Easy on the eyes for long sessions

## Example Interaction Flow

1. User opens application → Sees disconnected state
2. User clicks Connect → Button disables, status shows "Connecting..."
3. Connection succeeds → Status shows green "Connected to QM1"
4. User pastes XML → Real-time validation shows "✓ Valid XML"
5. User clicks Beautify → XML formatted with indentation
6. User clicks Send Message → Status shows "Message sent successfully"
7. Message appears in history → "→ 2023-12-23 12:30:45 [DEV.QUEUE.2] SENT"
8. User switches to receive panel → Clicks Receive Messages
9. Messages appear → Formatted with timestamps and IDs
10. Status bar updates → "Received 3 message(s) from DEV.QUEUE.1"

This professional UI design ensures users can efficiently interact with IBM MQ queues while having clear visibility of all operations and their status.
