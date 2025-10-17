#!/bin/env bash

# Install paparascii as a Clojure tool

echo "Installing paparascii as a Clojure tool..." >&2

# Install from local directory (for testing)
LOCAL_ROOT=$(pwd)
if clojure -Ttools install lambda-lifters/paparascii '{:local/root "'"${LOCAL_ROOT}"'"}' :as paparascii; then
cat <<EOS >&2
Installation complete!

Usage:
  cd your-site-directory
  clojure -Tpaparascii init        # Initialize new site
  clojure -Tpaparascii build       # Build site
  clojure -Tpaparascii serve       # Start dev server
  clojure -Tpaparascii new-post :title '"Post Title"'
EOS
else
cat <<EOS >&2
Installation failed :-(
EOS
fi