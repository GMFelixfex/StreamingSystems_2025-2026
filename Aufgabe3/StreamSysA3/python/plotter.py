from pathlib import Path
import json
import argparse
import sys

import plotly.graph_objects as go


def read_json_lines(path: Path):
	scans = []
	distances = []
	try:
		with path.open("r", encoding="utf-8") as f:
			for line in f:
				line = line.strip()
				if not line:
					continue
				try:
					obj = json.loads(line)
				except json.JSONDecodeError:
					# ignore malformed lines
					continue
				scans.append(obj.get("scan"))
				distances.append(obj.get("distance"))
	except FileNotFoundError:
		print(f"File not found: {path}", file=sys.stderr)
		return [], []
	return scans, distances


def make_plot(mq_path: Path, kafka_path: Path, out_path: Path):
	mq_x, mq_y = read_json_lines(mq_path)
	kf_x, kf_y = read_json_lines(kafka_path)

	fig = go.Figure()
	if mq_x:
		fig.add_trace(go.Scatter(x=mq_x, y=mq_y, mode="lines+markers", name="MQ"))
	if kf_x:
		fig.add_trace(go.Scatter(x=kf_x, y=kf_y, mode="lines+markers", name="Kafka"))

	fig.update_layout(
		title="Lidar scan distances (MQ vs Kafka)",
		xaxis_title="scan",
		yaxis_title="distance",
		legend_title="source",
		template="plotly_white",
	)

	out_path.parent.mkdir(parents=True, exist_ok=True)
	fig.write_html(str(out_path), include_plotlyjs="cdn")
	print(f"Wrote plot to: {out_path}")


def main():
	workspace_root = Path(__file__).resolve().parents[1]
	default_mq = workspace_root / "data" / "resultsMQ.txt"
	default_kafka = workspace_root / "data" / "resultsKafka.txt"
	default_out = workspace_root / "data" / "plot_results.html"

	p = argparse.ArgumentParser()
	p.add_argument("--mq", type=Path, default=default_mq, help="Path to resultsMQ.txt")
	p.add_argument("--kafka", type=Path, default=default_kafka, help="Path to resultsKafka.txt")
	p.add_argument("--out", type=Path, default=default_out, help="Output HTML file")
	args = p.parse_args()

	make_plot(args.mq, args.kafka, args.out)


if __name__ == "__main__":
	main()

