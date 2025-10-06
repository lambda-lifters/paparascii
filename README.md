# Ascii Blog

A fast, modern static site generator built with Clojure.

## Installation

### As a Clojure Tool (Recommended)

```bash
# Install from local directory (for development)
clojure -Ttools install lambda-lifters/paparascii \
  '{:local/root "/path/to/paparascii"}' \
  :as paparascii

# Or install from GitHub (when published)
# clojure -Ttools install io.github.lambda-lifters/paparascii \
#   '{:git/url "https://github.com/lambda-lifters/paparascii.git" 
#     :git/sha "LATEST_SHA"}' \
#   :as paparascii
```

## Usage

All commands are run from your site directory (containing `site-config.edn`):

```bash
# Initialize a new site
clojure -Tpaparascii init

# Clean the TARGET directory
clojure -Tpaparascii clean

# Build the site
clojure -Tpaparascii build

# Start development server
clojure -Tpaparascii serve

# Create a new blog post
clojure -Tpaparascii new-post :title '"My First Post"'

# List all posts
clojure -Tpaparascii list-posts
```

## Creating Aliases (Optional)

For convenience, you can create shorter aliases in your shell:

```bash
# Add to ~/.bashrc or ~/.zshrc
alias pa-build='clojure -Tpaparascii build'
alias pa-serve='clojure -Tpaparascii serve'
alias pa-clean='clojure -Tpaparascii clean'
```

## Site Structure

A valid paparascii site has this structure:

```
my-blog/
├── site-config.edn    # Site configuration
├── blog/              # Blog posts (.adoc files)
├── site/              # Site pages (about, contact, etc.)
├── resources/         # Static resources (404.html, etc.)
├── assets/            # CSS, JS, images
├── templates/         # Content templates
└── TARGET/            # Generated output (created by build)
```

## Quick Start

1. Create a new directory for your site
2. Run `clojure -Tpaparascii init`
3. Edit `site-config.edn` with your details
4. Create content in `blog/` and `site/`
5. Run `clojure -Tpaparascii build`
6. Run `clojure -Tpaparascii serve` to preview

## Features

- **Fast builds** using AsciidoctorJ
- **Beautiful output** with Bootstrap 5
- **Clean HTML** generated with Hiccup
- **Tag system** with automatic tag pages
- **Template support** for consistent content
- **Zero dependencies** for development server
- **Tool installation** - works from any directory

## License

MIT
