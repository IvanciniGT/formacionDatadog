#!/bin/sh
# Traffic generator. Mix of good, slow and failing requests so that every
# screen in Datadog has something to show. Runs forever.
BASE="${TARGET:-http://nginx}"
NAMES="ana luis marta pedro sofia hugo lucia"
hit() { curl -s -o /dev/null -w "%{http_code} %{time_total}s $1\n" "$@"; }
while true; do
  n=$(( $(date +%s) % 7 + 1 )); customer=$(echo $NAMES | cut -d' ' -f$n)
  product=$(( $(od -An -N1 -tu1 /dev/urandom) % 10 + 1 ))
  r=$(( $(od -An -N1 -tu1 /dev/urandom) % 100 ))
  hit "$BASE/api/products"
  hit "$BASE/api/products/$product"
  if   [ $r -lt 50 ]; then hit "$BASE/api/orders" -X POST -H 'Content-Type: application/json' -d "{\"customer\":\"$customer\",\"productId\":$product,\"quantity\":1}"
  elif [ $r -lt 65 ]; then hit "$BASE/api/orders"
  elif [ $r -lt 80 ]; then hit "$BASE/api/slow"
  elif [ $r -lt 88 ]; then hit "$BASE/api/payment"
  elif [ $r -lt 94 ]; then hit "$BASE/api/report"
  elif [ $r -lt 98 ]; then hit "$BASE/api/error"
  else                     hit "$BASE/api/products/999"
  fi
  hit "$BASE/"
  sleep "${PAUSE:-2}"
done
