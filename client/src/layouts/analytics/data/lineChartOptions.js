export const lineChartOptionsDashboard = {
  chart: {
    toolbar: { show: false },
  },
  tooltip: { theme: "light" },
  dataLabels: { enabled: false },
  stroke: { curve: "smooth", width: 4 },
  xaxis: {
    type: "datetime",
      categories: [
  "Day 0",
  "Day 1",
  "Day 2",
  "Day 3",
  "Day 4",
  "Day 5",
  "Day 6",
  "Day 7",
  "Day 8",
  "Day 9",
  "Day 10",
  "Day 11",
  "Day 12",
  "Day 13",
  "Day 14",
  "Day 15",
  "Day 16",
  "Day 17",
  "Day 18",
  "Day 19",
  "Day 20"
],
    labels: {
      style: { colors: "#FFF", fontSize: "12px", fontWeight: "600" },
    },
    axisBorder: { show: false },
    axisTicks: { show: false },
  },
  yaxis: {
    labels: { style: { colors: "#c8cfca", fontSize: "12px", fontWeight: "600" } },
  },
  legend: {
    show: true,
    position: "top",
    horizontalAlign: "center", // Align legend in the center
    fontSize: "16px", // General font size for legend
    fontWeight: 500, // Regular weight for legend text
    labels: {
      colors: "#FFFFFF", // Force white text color for legend
      useSeriesColors: false, // Prevent using series colors for legend text
    },
    markers: {
      size: 10, // Increase marker size slightly for better visibility
      fillColors: undefined, // Default to series colors for markers
      strokeWidth: 0, // Add a small stroke to markers
    },
    onItemClick: {
      toggleDataSeries: true, // Allow toggling data series on click
    },
    onItemHover: {
      highlightDataSeries: true, // Highlight series on hover
    },
  },
  grid: {
    show: true,
    strokeDashArray: 0,
    borderColor: "#56577A",
  },
  fill: { type: "solid", opacity: 0 },
  colors: [
      "#00FFFF",    // Cyan - Eliah
      "#ff8800",    // Orange - Sebastian
      "#1ABC9C",    // Teal - Paul
      "#E74C3C",    // Red - Nikita
      "#cb208e",    // Pink - Matthi
      "#2ECC71",    // Green - Markus
      "#3498DB",    // Blue - Luca
      "#ffee20",    // Yellow - Jura Jonas
      "#8E44AD",    // Purple - Jonas
      "#FF00FF",    // Magenta
      "#32CD32",    // Lime Green
      "#FF1493",    // Deep Pink
      "#00CED1",    // Dark Turquoise
      "#FF4500",    // Orange Red
      "#4682B4",    // Steel Blue
      "#9370DB"],   // Medium Purple
};