#!/bin/env bash

# Install ascii-blog as a Clojure tool

echo "Installing ascii-blog as a Clojure tool..."

# Install from local directory (for testing)
clojure -Ttools install-latest \
  :lib lambda-lifters/ascii-blog \
  :local/root "\"$(pwd)\"" \
  :as ascii-blog

echo "Installation complete!"
echo ""
echo "Usage:"
echo "  cd your-site-directory"
echo "  clojure -Tascii-blog init        # Initialize new site"
echo "  clojure -Tascii-blog build       # Build site"
echo "  clojure -Tascii-blog serve       # Start dev server"
echo "  clojure -Tascii-blog new-post :title '\"Post Title\"'"
