# Professional Tab Styling - Visual Guide

## Overview
The tabs have been redesigned with professional styling to improve visibility and user experience.

## Tab Appearance

### Tab Bar Layout
```
┌─────────────────────────────────────────────────────────────────┐
│  Tab Header Area (Dark background #1e1e1e)                      │
│  ┌──────────────────────────┐  ┌──────────────────────────┐    │
│  │ 📋 Configured Connection │  │ 🔧 Dynamic Configuration │    │
│  │  (Selected - Green)      │  │  (Unselected - Gray)     │    │
│  └──────────────────────────┘  └──────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Tab Content Area                                                │
│  (Main application content)                                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Tab States

### 1. Selected Tab
- **Background**: #2b2b2b (matches content area)
- **Border**: 2px solid #4CAF50 (green accent)
- **Text Color**: #4CAF50 (green)
- **Font**: 13px Bold
- **Height**: 40px
- **Border Radius**: 5px (top corners only)
- **Padding**: 8px 20px

**Visual Representation:**
```
┌─────────────────────────────┐
│  📋 Configured Connection   │  ← Green border on top/sides
│  (Green text, Bold)         │     Dark background
└─────────────────────────────┘
```

### 2. Unselected Tab
- **Background**: #3e3e3e (dark gray)
- **Border**: 1px solid #555555 (subtle gray)
- **Text Color**: #b0b0b0 (light gray)
- **Font**: 13px Bold
- **Height**: 40px
- **Border Radius**: 5px (top corners only)
- **Padding**: 8px 20px

**Visual Representation:**
```
┌─────────────────────────────┐
│  🔧 Dynamic Configuration   │  ← Subtle gray border
│  (Light gray text, Bold)    │     Gray background
└─────────────────────────────┘
```

### 3. Hover State
- **Background**: #4e4e4e (lighter gray)
- **Other properties**: Same as unselected
- **Effect**: Subtle brightening on mouse over

**Visual Representation:**
```
┌─────────────────────────────┐
│  🔧 Dynamic Configuration   │  ← Lighter background
│  (Light gray text, Bold)    │     on hover
└─────────────────────────────┘
```

## Color Palette

| Element | Color | Purpose |
|---------|-------|---------|
| Selected Tab Background | `#2b2b2b` | Matches content area |
| Selected Tab Border | `#4CAF50` | Green accent (active) |
| Selected Tab Text | `#4CAF50` | Green text (active) |
| Unselected Tab Background | `#3e3e3e` | Dark gray |
| Unselected Tab Border | `#555555` | Subtle border |
| Unselected Tab Text | `#b0b0b0` | Light gray |
| Hover Background | `#4e4e4e` | Lighter gray |
| Tab Header Area | `#1e1e1e` | Very dark background |

## Icons

| Tab | Icon | Meaning |
|-----|------|---------|
| Configured Connection | 📋 | Clipboard/configuration file |
| Dynamic Configuration | 🔧 | Wrench/tools (dynamic/manual) |

## Typography

- **Font Size**: 13px (larger for better readability)
- **Font Weight**: Bold (enhanced visibility)
- **Text Style**: Unicode icons + text labels

## Spacing

- **Tab Height**: 40px (increased from default)
- **Horizontal Padding**: 20px (comfortable spacing)
- **Vertical Padding**: 8px (centered text)
- **Tab Header Padding**: 5px 10px 0 10px

## CSS Rules Applied

### Tab Container
```css
.tab-pane {
    -fx-background-color: #2b2b2b;
    -fx-border-color: #3e3e3e;
    -fx-border-width: 1px;
}
```

### Tab Header
```css
.tab-pane .tab-header-area {
    -fx-background-color: #1e1e1e;
    -fx-padding: 5 10 0 10;
}
```

### Individual Tab (Default)
```css
.tab-pane .tab {
    -fx-background-color: #3e3e3e;
    -fx-background-radius: 5 5 0 0;
    -fx-border-color: #555555;
    -fx-border-width: 1 1 0 1;
    -fx-padding: 8 20 8 20;
}
```

### Selected Tab
```css
.tab-pane .tab:selected {
    -fx-background-color: #2b2b2b;
    -fx-border-color: #4CAF50;
    -fx-border-width: 2 2 0 2;
}
```

### Hover State
```css
.tab-pane .tab:hover {
    -fx-background-color: #4e4e4e;
}
```

### Tab Labels
```css
.tab-pane .tab .tab-label {
    -fx-text-fill: #b0b0b0;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
}

.tab-pane .tab:selected .tab-label {
    -fx-text-fill: #4CAF50;
}
```

## User Experience Improvements

### Before
- Small tabs (default height ~25px)
- Low contrast between selected/unselected
- Default font size (smaller)
- Plain text labels
- Less professional appearance

### After
- Larger tabs (40px height)
- Clear visual distinction with green accent
- Larger bold font (13px)
- Icons for quick recognition
- Professional modern look
- Better visibility on all displays
- Consistent with application theme

## Benefits

1. **Improved Visibility**: Larger tabs are easier to see and click
2. **Clear Selection**: Green border and text make active tab obvious
3. **Professional Look**: Modern design with rounded corners and proper spacing
4. **Better UX**: Icons provide quick visual cues
5. **Accessibility**: Higher contrast and larger text
6. **Theme Consistency**: Matches overall dark theme
7. **Visual Feedback**: Hover states provide interaction feedback

## Implementation Notes

- Tabs are set to `UNAVAILABLE` close policy (cannot be closed)
- Tab height is fixed at 40px for consistency
- CSS is loaded from `styles.css` resource file
- Icons are Unicode characters (no image files needed)
- Green accent (#4CAF50) matches success/active states throughout app
