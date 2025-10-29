# HexText

HexText extends Minecraft's font renderer with a modern colour pipeline that understands full RGB
hex codes, nested colour spans, and a handful of animated text effects. This document summarises the
available formatting tokens and how they interact so you can take full advantage of the system.

## Standard Minecraft formatting

All of the vanilla `§` style formatting codes continue to work exactly as they do in base Minecraft.
When using HexText you may prefer to write them with an ampersand for easier typing; HexText will
normalise them for the game at render time. The supported codes are:

| Token        | Description                                     |
| ------------ | ----------------------------------------------- |
| `&0`–`&9`, `&a`–`&f` | Vanilla palette colours (16 in total). |
| `&k`         | Obfuscated/random glyphs.                        |
| `&l`         | Bold text.                                       |
| `&m`         | Strikethrough.                                   |
| `&n`         | Underline.                                       |
| `&o`         | Italic text.                                     |
| `&r`         | Reset colour and clear styles/effects.           |

You can still type the same codes with `§` instead of `&` if you prefer the original syntax.

## Direct RGB colours

HexText recognises two RGB-focused syntaxes that let you move beyond the vanilla palette:

### Inline hex colours

Use `&RRGGBB` to immediately switch to an exact RGB colour (for example `&ff8800`). The change takes
effect from the character immediately following the code and clears any nested colour spans that were
active before it.

### Scoped hex spans

Wrap a section of text in `<RRGGBB> ... </RRGGBB>` to apply a colour until the matching closing tag.
These spans can be nested and work alongside inline hex codes. Closing a span restores the previous
colour, making it easy to temporarily highlight text without permanently overwriting the stack.

Both syntaxes accept upper- or lower-case hexadecimal digits.

## Special effects

In addition to vanilla style toggles, HexText adds four extra effect codes. Like other formatting
codes they can be written with either `&` or `§`:

| Token | Effect                                                                 |
| ----- | ---------------------------------------------------------------------- |
| `&g`  | Enables the animated rainbow shader. Use `&r` to clear it.             |
| `&h`  | Flips the text upside-down (Dinnerbone effect).                         |
| `&i`  | Adds a fiery ignite overlay to the text.                                |
| `&j`  | Applies a subtle shake/jitter animation.                                |

Effects remain active until the renderer resets them. Any explicit reset (`&r`) or colour-stack
change (such as applying a new colour or closing a `<RRGGBB>` span) clears the effect, so place the
code after the colour you want it to animate.

## Tips for authors

* Hex colour codes are always six hexadecimal digits – shorthand (`#abc`) is not supported.
* Nesting `<RRGGBB>` spans is safe; the renderer keeps an internal colour stack and restores the
  previous colour when a span closes.
* You can freely combine RGB colours, vanilla colour codes, and style/effect toggles in a single
  string. HexText will resolve the intended output before rendering.

With these tokens you can express complex gradients, highlight sections of text, or animate chat
messages while remaining compatible with Minecraft's existing formatting system.
