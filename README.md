# Ascii Blog

A fast, modern static site generator built with Clojure.

## Installation

### As a Clojure Tool (Recommended)

```bash
# Install from local directory (for development)
clojure -Ttools install lambda-lifters/ascii-blog \
  '{:local/root "/path/to/ascii-blog"}' \
  :as ascii-blog

# Or install from GitHub (when published)
# clojure -Ttools install io.github.lambda-lifters/ascii-blog \
#   '{:git/url "https://github.com/lambda-lifters/ascii-blog.git" 
#     :git/sha "LATEST_SHA"}' \
#   :as ascii-blog
```

## Usage

All commands are run from your site directory (containing `site-config.edn`):

```bash
# Initialize a new site
clojure -Tascii-blog init

# Clean the TARGET directory
clojure -Tascii-blog clean

# Build the site
clojure -Tascii-blog build

# Start development server
clojure -Tascii-blog serve

# Create a new blog post
clojure -Tascii-blog new-post :title '"My First Post"'

# List all posts
clojure -Tascii-blog list-posts
```

## Creating Aliases (Optional)

For convenience, you can create shorter aliases in your shell:

```bash
# Add ao ~/.bashrc or ~/.zshrc
alias ab-build='clojure -Tascii-blog build'
alias ab-serve='clojure -Tascii-blog serve'
alias ab-clean='clojure -Tascii-blog clean'
```

## Site Structure

A valid ascii-blog site has this structure:

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
2. Run `clojure -Tascii-blog init`
3. Edit `site-config.edn` with your details
4. Create content in `blog/` and `site/`
5. Run `clojure -Tascii-blog build`
6. Run `clojure -Tascii-blog serve` to preview

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
