package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.application.port.out.WorkOrderMetrics;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcWorkOrderMetrics implements WorkOrderMetrics {

	private static final String TIME_PER_ORDER_AND_STATUS = """
			with segments as (
			    select h.work_order_id, h.to_status as status, h.occurred_at as started,
			           lead(h.occurred_at) over (partition by h.work_order_id order by h.occurred_at, h.id) as ended
			    from workorder.work_order_status_history h
			), completed as (
			    select distinct work_order_id from workorder.work_order_status_history
			    where to_status = 'COMPLETED' and occurred_at >= ? and occurred_at < ?
			), per_order as (
			    select s.work_order_id, s.status, sum(extract(epoch from (s.ended - s.started))) as seconds
			    from segments s join completed c on c.work_order_id = s.work_order_id
			    where s.ended is not null
			    group by s.work_order_id, s.status
			)
			""";

	private final JdbcClient jdbc;

	JdbcWorkOrderMetrics(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Result completedBetween(Instant from, Instant to) {
		Timestamp start = Timestamp.from(from);
		Timestamp end = Timestamp.from(to);
		long completed = jdbc.sql("select count(distinct work_order_id) from workorder.work_order_status_history "
						+ "where to_status = 'COMPLETED' and occurred_at >= ? and occurred_at < ?")
				.params(start, end).query(Long.class).single();
		Double execution = jdbc.sql(TIME_PER_ORDER_AND_STATUS + "select avg(seconds) from per_order where status = 'IN_EXECUTION'")
				.params(start, end).query(Double.class).optional().orElse(null);
		List<StatusTime> byStatus = jdbc.sql(TIME_PER_ORDER_AND_STATUS
						+ "select status, count(*) as orders, avg(seconds) as average from per_order group by status order by status")
				.params(start, end)
				.query((rs, i) -> new StatusTime(WorkOrderStatus.valueOf(rs.getString("status")), rs.getLong("orders"),
						toDuration(rs.getDouble("average"))))
				.list();
		return new Result(completed, execution == null ? Duration.ZERO : toDuration(execution), byStatus);
	}

	private static Duration toDuration(double seconds) {
		return Duration.ofMillis(Math.round(seconds * 1000));
	}
}
