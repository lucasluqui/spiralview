# spiralview
> [!NOTE]
> This repository no longer hosts the latest release of spiralview as it has been integrated into [Knight Launcher](https://github.com/lucasluqui/KnightLauncher). Please [download Knight Launcher](https://github.com/lucasluqui/KnightLauncher/releases/latest) to use spiralview. You will only find the latest **source code** here, not releases.

Wrapper of various Three Rings editors for Spiral Knights.

spiralview relies on the [clyde](https://github.com/threerings/clyde) library to work, 99.9% of the work is done there, 
this is simply a wrapper for it with some minor quality of life changes and fixes for [Spiral Knights](https://spiralknights.com) in specific.

## Building From Source
To build spiralview follow these steps:

1. Prerequisites 
   - Java 21 JDK installed.
   - [Maven](https://maven.apache.org/download.cgi) installed:
     - **Windows**: Download the ZIP from the link above, extract, then add `bin/` to your `PATH`.
     - **macOS (Homebrew)**: `brew install maven`.
     - **Linux (APT)**: `apt install maven`.
   - [Git](https://git-scm.com/downloads) installed:
     - **Windows**: Download the installer from [git-scm.com](https://git-scm.com/downloads) and follow the setup.
     - **macOS (Homebrew)**: `brew install git`.
     - **Linux (APT)**: `apt install git`.
2. Clone the repository.
   - `git clone https://github.com/lucasluqui/spiralview.git`
3. Copy `projectx-pcode.jar` from your Spiral Knights `code` directory into the project's `lib` directory.
4. Validate all Maven dependencies.
   - `mvn validate`
5. Build the package using Maven.
   - `mvn clean package`
6. Copy the package built by Maven to your Knight Launcher project and then re-build Knight Launcher.

## Thanks
- **rrrrex** (rrrrex, 357940678489014284 @ Discord): For in the past providing local repositories to build clyde and updated configs.
- **onyxbits** (spiral.onyxbits.de): Author of spiralspy; predecessor of spiralview.
