# Zer0ne-OCR

A modern desktop application for extracting invoice data from images and PDFs using AI-powered OCR, built with Kotlin Multiplatform and Compose Desktop.

## Features

- 🖼️ **Invoice OCR**: Extract invoice data from images (PNG, JPG, JPEG) and PDFs
- 📊 **Excel Export**: Automatically export extracted data to Excel files
- 🔄 **PDF Tools**: Convert PDFs to images and vice versa
- 📁 **Batch Processing**: Process multiple files at once
- 🎨 **Modern UI**: JetBrains Toolbox-inspired design with dark/light theme support
- 🔑 **API Key Rotation**: Automatically rotate through multiple Groq API keys to avoid rate limits

## Prerequisites

- JDK 17 or higher
- [Groq API Key(s)](https://console.groq.com/keys)

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/aaitelka/Zer0ne-OCR.git
cd Zer0ne-OCR
```

### 2. Configure API Keys

Copy the example file and add your Groq API keys:

```bash
cp api_keys.txt.example api_keys.txt
```

Edit `api_keys.txt` and add your API keys (one per line):

```
gsk_your_first_api_key_here
gsk_your_second_api_key_here
gsk_your_third_api_key_here
```

> ⚠️ **Important**: Never commit `api_keys.txt` to version control. It's already in `.gitignore`.

#### API Keys File Location (for installed applications)

The application searches for `api_keys.txt` in the following locations (in order):

**Windows:**
1. `C:\Users\{username}\.zer0ne-ocr\api_keys.txt` (recommended)
2. Application installation directory
3. `%LOCALAPPDATA%\Zer0ne-OCR\api_keys.txt`
4. `%APPDATA%\Zer0ne-OCR\api_keys.txt`
5. Desktop folder

**macOS/Linux:**
1. `~/.zer0ne-ocr/api_keys.txt` (recommended)
2. Current working directory
3. Application installation directory

### 3. Build and Run

**macOS/Linux:**
```bash
./gradlew :composeApp:run
```

**Windows:**
```bash
.\gradlew.bat :composeApp:run
```

## Usage

1. **Convert Tab**: Drop invoice images/PDFs to extract data and export to Excel
2. **PDF Tools Tab**: Convert between PDF and image formats
3. **History Tab**: View previously exported Excel files

## Project Structure

```
composeApp/
└── src/
    └── jvmMain/
        └── kotlin/
            └── ma/zer0ne/ocr/
                ├── App.kt              # Main application
                ├── config/             # Configuration management
                ├── model/              # Data models
                ├── processors/         # PDF and image processors
                ├── services/           # API services
                ├── ui/                 # UI components
                │   ├── components/     # Reusable components
                │   ├── screens/        # App screens
                │   └── theme/          # Theme and colors
                └── utils/              # Utility functions
```

## Technologies

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Desktop](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Groq API](https://groq.com/) (Llama 4 Maverick model)
- [Apache POI](https://poi.apache.org/) for Excel export
- [Apache PDFBox](https://pdfbox.apache.org/) for PDF processing

## License

MIT License
