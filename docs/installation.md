# Installation

This guide shows how to install **Kotlin Toolbox** in IntelliJ IDEA.

---

## Requirements

Before installing, make sure you have:

- **IntelliJ IDEA** 2025.3.2 or later
- **Kotlin plugin** enabled (usually pre-installed)
- Internet connection (for Marketplace installation)

---

## Method 1: Via IDE (Recommended)

The simplest way to install the plugin is directly from IntelliJ IDEA:

### Step by Step

1. Open Settings
2. From the menu: `Settings`
3. Navigate to Plugins
4. Click the **Marketplace** tab
5. Search for **Kotlin Toolbox**
6. Select the plugin from the results list
7. Click **Install** button
8. Wait for the download and installation
9. Click **Restart IDE** when prompted

!!! tip "Screenshot Placeholder"
    Add a screenshot of the process at `docs/assets/images/installation/marketplace.png`

<!-- ![Installation via Marketplace](assets/images/installation/marketplace.png) -->

---

## Method 2: Via JetBrains Marketplace

You can also install from the JetBrains website:

[Open on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30324-kotbox){ .md-button .md-button--primary }

There are two ways to install from the Marketplace page:

### Option A: Install button (IDE running)

If your IDE is already open:

1. Click the **Install** button on the Marketplace page
2. The plugin will be installed directly into your running IDE
3. Restart the IDE when prompted

### Option B: Get button (install from disk)

If you prefer to download the file first:

1. Click the **Get** button
2. Choose the version compatible with your IDE
3. Download the file
4. Open your IDE and press `Ctrl+Alt+S` to open Settings
5. Go to **Plugins** → gear icon ⚙️ → **Install Plugin from Disk...**
6. Select the downloaded file and click **OK**
7. Restart the IDE when prompted

---

## Method 3: Manual Installation via GitHub

To install a specific version or for offline installation:

### Download

1. Visit the [Releases page on GitHub](https://github.com/andrelmv/kotbox/releases/latest)
2. Download the `.zip` file for the desired version
3. **Do not unzip the file**

### Installation

1. Open **Settings/Preferences** → **Plugins**
2. Click the gear icon ⚙️
3. Select **Install Plugin from Disk...**
4. Navigate to the downloaded `.zip` file
5. Click **OK**
6. Restart the IDE when prompted

!!! warning "Attention"
    Keep the `.zip` file compressed. Do not extract its contents before installing.

---

## Verifying the Installation

After restarting the IDE, verify the plugin was installed correctly:

### 1. Check the Plugin List

- Go to **Settings/Preferences** → **Plugins** → **Installed**
- Look for "**Kotlin Toolbox**" in the list
- Make sure it is **enabled** (checkbox checked)

### 2. Open the Tool Window

- From the menu, go to **View** → **Tool Windows**
- You should see the **Kotlin Toolbox** option
- Click to open the **Kotlin Toolbox** window with the tools

!!! success "Installation Complete!"
    If you can see the tool window, the installation was successful!

<!-- ![Tool Window](assets/images/installation/tool-window.png) -->

---

## Next Steps

Now that the plugin is installed, explore the features:

<div class="grid cards" markdown>

-   :material-lock-check: **[JWT Encoder/Decoder](features/jwt-encoder-decoder.md)**

    Learn how to encode and decode JWT tokens

-   :material-lightbulb-on: **[Kotlin Inlay Hints](features/kotlin-inlay-hints.md)**

    Configure visual hints in your Kotlin code

</div>

---

## Troubleshooting

### Plugin does not appear in the Marketplace

- Check your internet connection
- Make sure you are using IntelliJ IDEA 2025.3.2+
- Try refreshing the plugin list: **Settings** → **Plugins** → gear icon → **Check for Plugin Updates**

### Installation error

- Check that you have enough disk space
- Try installing via the manual method
- Check the IDE logs: **Help** → **Show Log in Finder/Explorer**

### Tool Window does not appear

- Verify the plugin is enabled in **Settings** → **Plugins**
- Restart the IDE completely
- If the problem persists, [open an issue](https://github.com/andrelmv/kotbox/issues)

---

## Uninstalling

If you need to uninstall the plugin:

1. Go to **Settings/Preferences** → **Plugins**
2. Find "**Kotlin Toolbox**" in the **Installed** list
3. Click the gear icon next to the plugin
4. Select **Uninstall**
5. Restart the IDE

---

## Updates

The plugin is updated automatically through the JetBrains Marketplace. To check for updates manually:

1. **Settings/Preferences** → **Plugins**
2. Click the gear icon ⚙️
3. Select **Check for Plugin Updates**

!!! info "Notifications"
    By default, IntelliJ notifies you when updates are available for your plugins.

---

<div style="text-align: center;" markdown="1">

**Installation complete?**

[Explore Features](features/jwt-encoder-decoder.md){ .md-button .md-button--primary }

</div>
