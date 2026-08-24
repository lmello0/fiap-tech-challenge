# Stock Policy separates the low-stock threshold from the auto-order behavior

`ReorderRule` used to bundle two decisions into one row: the quantity at which a part counts as low
(`minQuantity`), and the standing instruction to automatically place a purchase order up to
`maxQuantity` with a specific `vendor`. Turning auto-ordering off (`enabled = false`) didn't separate
those — it just left `maxQuantity` and `vendor` sitting there unused, and a part being "low" was
undefined for any part that had no rule at all, including one sitting at zero.

`StockPolicy` (renamed from `ReorderRule`) keeps one flag, `autoReorderEnabled`, and makes
`maxQuantity`/`vendor` nullable, required only when that flag is true (`chk_auto_reorder_requires_max_and_vendor`).
A policy with `autoReorderEnabled = false` is now a complete, meaningful state: "this part has a
low-stock threshold worth tracking, but nobody has decided — or nobody wants — an automatic order
placed against it." A `StockStatus` of `LOW`/`OK` is computed off `minQuantity` alone; `OUT` is
available-quantity-based and doesn't need a policy at all; `NO_POLICY` is the fourth state for a part
nobody has set a threshold for, which is deliberately distinct from `OK` — a part sitting at zero with
no policy is the most urgent row on a stock list and must not look the same as a part that's actually
fine.

We considered two flags (a dormant/active switch on the row itself, separate from auto-reorder) and
rejected it: there's no real state "track this part's threshold but don't ever tell anyone it's low"
that isn't just "delete the policy." We also considered making the vendor foreign key itself the
switch (no vendor ⇒ threshold-only) and rejected that too — it overloads a reference meant to answer
"who do we buy this from" with a policy decision, and it makes "we buy this from Vendor X but restock
it by hand" unrecordable.

A `vendorId` supplied on create/update is validated even when `autoReorderEnabled` is false — a bad
reference fails the request rather than being silently discarded — but only actually stored (and only
required at all) when auto-reorder is on.
