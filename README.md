## 👋 Welcome to the HexText Repository.

HexText upgrades Minecraft’s font renderer with full RGB color support, nested color spans, and a few animated text effects.
If you’ve ever wanted more color control or wanted your text to move, glow, or shake, this mod will do it. It also adds a
better handling for Signs, Anvils and Chat if installed on the server.

----------------

<a href="https://discord.gg/pQqRTvFeJ5"> <img src="https://img.shields.io/badge/KAMKEEL_Discord-7289DA?style=for-the-badge&logo=discord&logoColor=white" width="400" height="60"> </a>
<a href="https://ko-fi.com/kamkeel"> <img src="https://img.shields.io/badge/Support_Me_|_Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white" alt="Support Me"  width="400" height="60"> </a>

[![Download CustomNPC+](https://img.shields.io/badge/CustomNPC+-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://modrinth.com/mod/customnpc-plus)
[![Download MPM+](https://img.shields.io/badge/MorePlayerModels+-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/moreplayermodels-plus)
[![Download PluginMod](https://img.shields.io/badge/Plugin_Mod-0081CB?style=for-the-badge&logo=material-ui&logoColor=white)](https://github.com/KAMKEEL/Plugin-Mod)

----------------

### ⬇️ Downloads
- **Modrinth**: [Link](https://legacy.curseforge.com/minecraft/mc-mods/hex-text)
- **CurseForge**: [Link](https://legacy.curseforge.com/minecraft/mc-mods/hex-text)

### 🔹 Installation
This mod requires **UniMixins** to run. It can be installed on both the Server and Client.

----------------

## Standard Minecraft formatting

All the regular Minecraft § codes still work exactly like before.
If you prefer typing & instead, HexText will handle it automatically if your server allows it.

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

Use `&#RRGGBB` (or `§#RRGGBB`) to immediately switch to an exact RGB colour (for example
`&#ff8800`). The change takes effect from the character immediately following the code and clears any
nested colour spans that were active before it.

### Scoped hex spans

Wrap a section of text in `<RRGGBB> ... </RRGGBB>` to apply a colour until the matching closing tag.
These spans can be nested and work alongside inline hex codes. Closing a span restores the previous
colour, making it easy to temporarily highlight text without permanently overwriting the stack.
Servers can disable these HTML-style tags if they want to restrict formatting to inline codes.

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

## Server configuration

HexText exposes several server-side toggles via the `server` category of its configuration file:

* `enableRgbHtmlFormat` – when `true`, the `<RRGGBB>` and `</RRGGBB>` tags described above are parsed.
  Set this to `false` to treat them as literal text.
* `allowSignEditing` – controls whether players can right-click signs with an empty hand to open the
  editor.
* `universalAmpersandFormatting` – accepts ampersands as an alternative to the section sign for
  formatting codes and direct RGB colours.
* `ampersandsInChat` – converts ampersand formatting tokens to section signs when sending chat
  messages or commands.
* `ampersandsInSigns` – converts ampersand formatting tokens to section signs while editing signs.
* `ampersandsInRepairs` – converts ampersand formatting tokens to section signs when renaming items
  in anvils.
