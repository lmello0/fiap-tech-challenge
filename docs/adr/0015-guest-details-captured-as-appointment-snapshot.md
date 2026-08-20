# Guest details are captured as an inline Appointment snapshot, not shadow entities

`Vehicle.customerId` is a required column, and there is no path to a `Customer` facet without a real
`User` behind it. A Guest booking a Drop-off Appointment has neither, and may never get either if
they never return. The obvious alternative — create placeholder `User`/`Customer`/`Vehicle` rows the
instant a Guest books, flagged somehow as unverified or pending — was rejected: every module that
reads `user` or `vehicle` data would have to start accounting for rows that might not represent a
real person, undermining the exact invariant (a `Vehicle` always has a real owning `Customer`) those
modules already depend on.

Instead, a Guest's name, phone, email, and vehicle maker/model/year live only as fields on the
Drop-off `Appointment` itself. No `User`, `Customer`, or `Vehicle` row exists until `Guest
Conversion` actually happens. The cost is that a Drop-off Appointment's shape isn't uniform — it
carries either the guest fields or a real `customerId`/`vehicleId`, depending on who booked it —
rather than every Appointment pointing at real ids the same way.
