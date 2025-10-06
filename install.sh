#!/bin/env bash

# Install paparascii as a Clojure tool

echo "Installing paparascii as a Clojure tool..."

# Install from local directory (for testing)
clojure -Ttools install-latest \
  :lib lambda-lifters/paparascii \
  :local/root "\"$(pwd)\"" \
  :as paparascii

echo "Installation complete!"
echo ""
echo "Usage:"
echo "  cd your-site-directory"
echo "  clojure -Tpaparascii init        # Initialize new site"
echo "  clojure -Tpaparascii build       # Build site"
echo "  clojure -Tpaparascii serve       # Start dev server"
echo "  clojure -Tpaparascii new-post :title '\"Post Title\"'"
