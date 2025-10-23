# VS Code Setup for SentinelCore

## Quick Access

### Keyboard Shortcuts
- **Build**: `Ctrl+Shift+B` (default build task)
- **Clean Build**: `Ctrl+Alt+B`
- **Spotless Apply**: `Ctrl+Alt+S`
- **Run Server**: `Ctrl+Shift+R`
- **Run Client**: `Ctrl+Shift+C`
- **Run Tests**: `Ctrl+Shift+T`

### Command Palette Tasks
Press `Ctrl+Shift+P` and type "Tasks: Run Task", then choose:
- Build
- Clean Build
- Spotless Apply
- Spotless Check
- Run Server
- Run Client
- Clean
- Generate Sources
- Test

### Testing Area
The **Testing** sidebar (beaker icon) shows Java test classes when available. You can:

- Run individual tests
- Debug tests
- View test coverage

### Debugging

The **Run and Debug** panel (Ctrl+Shift+D) provides two configurations:

- **Debug Minecraft Server** - Attaches to server on port 5005
- **Debug Minecraft Client** - Attaches to client on port 5006

To debug:
1. Press `Ctrl+Shift+R` (server) or `Ctrl+Shift+C` (client) to start
2. Wait for "Listening for transport dt_socket at address: XXXX" message
3. Press F5 and select the appropriate debug configuration
4. Set breakpoints and debug normally

## Available Tasks

### Build Tasks
- **Build** - Standard build (`./gradlew build`)
- **Clean Build** - Clean then build (`./gradlew clean build`)
- **Spotless Apply** - Auto-format code
- **Spotless Check** - Check formatting
- **Clean** - Remove build artifacts
- **Generate Sources** - Generate Minecraft sources for IDE

### Run Tasks
- **Run Server** - Start Minecraft server (background)
- **Run Client** - Start Minecraft client (background)
- **Test** - Run all unit tests

## Status Bar Integration
- Click the status bar items to access common tasks
- Build errors will appear in the Problems panel
- Terminal output appears in dedicated panels

## Recommended Extensions
Install recommended extensions when prompted, or manually:
- Java Extension Pack
- Gradle for Java
- YAML
- GitLens
- Error Lens

## Tips
1. **Stop Server/Client**: Click the trash icon in the terminal panel
2. **View Logs**: Check the Terminal panel for output
3. **Problems Panel**: `Ctrl+Shift+M` to view build errors
4. **Quick Build**: `Ctrl+Shift+B` builds without opening terminal
