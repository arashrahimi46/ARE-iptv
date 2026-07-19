// ARE iptv — mock data for the UI kit. Real-looking IPTV content; images via picsum.
window.AREDATA = (() => {
  const img = (s, w, h) => `https://picsum.photos/seed/${s}/${w}/${h}`;
  const poster = (s) => img(s, 320, 480);
  const still = (s) => img(s, 480, 270);
  const wide = (s) => img(s, 1280, 640);

  const featured = {
    kicker: "Featured tonight", title: "British Grand Prix",
    meta: "LIVE · Sky Sports F1 · 4K HDR · Lap 34 / 52",
    synopsis: "Lights out at Silverstone as the championship battle reaches its halfway point. Full race, pit-wall audio and onboard cameras.",
    image: wide("silverstone-f1"), logo: "F1",
  };

  const continueWatching = [
    { title: "The Last of Us", meta: "S2 · E4 · 26 min left", image: still("lastofus"), progress: 45, remaining: "26 min left" },
    { title: "Dune: Part Two", meta: "1h 22m left", image: still("dune2"), progress: 38, remaining: "1h 22m left" },
    { title: "Slow Horses", meta: "S4 · E2 · 12 min left", image: still("slowhorses"), progress: 78, remaining: "12 min left" },
    { title: "Formula 1: Drive to Survive", meta: "S6 · E7", image: still("dts"), progress: 60, remaining: "18 min left" },
    { title: "Shōgun", meta: "S1 · E9", image: still("shogun"), progress: 25, remaining: "42 min left" },
  ];

  const liveNow = [
    { channel: "BBC One HD", number: "101", now: "The News at Ten", next: "Match of the Day", progress: 70, health: "stable", quality: "FHD", codec: "H.264", catchup: true, fav: true },
    { channel: "Sky Sports F1", number: "406", now: "British Grand Prix", next: "Post-race analysis", progress: 55, health: "stable", quality: "FHD", codec: "H.265", catchup: true, fav: true },
    { channel: "ESPN", number: "204", now: "NBA: Lakers @ Celtics", next: "SportsCenter", progress: 40, health: "moderate", quality: "HD", codec: "H.264", catchup: true },
    { channel: "CNN", number: "311", now: "Global News Hour", next: "Amanpour", progress: 62, health: "stable", quality: "FHD", codec: "H.264", catchup: true },
    { channel: "Discovery", number: "520", now: "How It's Made", next: "Gold Rush", progress: 18, health: "poor", quality: "SD", codec: "H.264" },
    { channel: "Cartoon Network", number: "701", now: "Adventure Time", next: "Regular Show", progress: 88, health: "stable", quality: "HD", codec: "H.264" },
  ];

  // Channel pool tagged by live category — drives the Live TV screen's category filter.
  const liveChannelPool = [
    { channel: "Sky Sports F1", number: "406", now: "British Grand Prix", next: "Post-race analysis", progress: 55, health: "stable", cat: "Sports", quality: "FHD", codec: "H.265", catchup: true, fav: true },
    { channel: "ESPN", number: "204", now: "NBA: Lakers @ Celtics", next: "SportsCenter", progress: 40, health: "moderate", cat: "Sports", quality: "HD", codec: "H.264", catchup: true },
    { channel: "TNT Sports 1", number: "410", now: "Premier League Live", next: "Goals on Sunday", progress: 33, health: "stable", cat: "Sports", quality: "FHD", codec: "H.265", catchup: true },
    { channel: "Eurosport 1", number: "412", now: "Cycling: Tour Stage 12", next: "Snooker", progress: 61, health: "stable", cat: "Sports", quality: "HD", codec: "H.264" },
    { channel: "BBC One HD", number: "101", now: "The News at Ten", next: "Match of the Day", progress: 70, health: "stable", cat: "Entertainment", quality: "FHD", codec: "H.264", catchup: true, fav: true },
    { channel: "ITV1 HD", number: "103", now: "Coronation Street", next: "Drama Premiere", progress: 48, health: "stable", cat: "Entertainment", quality: "FHD", codec: "H.264", catchup: true },
    { channel: "Channel 4 HD", number: "104", now: "Grand Designs", next: "Feature Film", progress: 22, health: "moderate", cat: "Entertainment", quality: "HD", codec: "H.264", catchup: true },
    { channel: "CNN", number: "311", now: "Global News Hour", next: "Amanpour", progress: 62, health: "stable", cat: "News", quality: "FHD", codec: "H.264", catchup: true },
    { channel: "BBC News", number: "231", now: "Verified Live", next: "The Context", progress: 15, health: "stable", cat: "News", quality: "HD", codec: "H.264", catchup: true },
    { channel: "Al Jazeera", number: "235", now: "Newshour", next: "Inside Story", progress: 44, health: "stable", cat: "News", quality: "HD", codec: "H.264" },
    { channel: "Discovery", number: "520", now: "How It's Made", next: "Gold Rush", progress: 18, health: "poor", cat: "Documentary", quality: "SD", codec: "H.264" },
    { channel: "National Geographic", number: "525", now: "Air Crash Investigation", next: "Drain the Oceans", progress: 71, health: "stable", cat: "Documentary", quality: "FHD", codec: "H.265", catchup: true },
    { channel: "Cartoon Network", number: "701", now: "Adventure Time", next: "Regular Show", progress: 88, health: "stable", cat: "Kids", quality: "HD", codec: "H.264" },
    { channel: "Nickelodeon", number: "705", now: "SpongeBob", next: "The Loud House", progress: 52, health: "stable", cat: "Kids", quality: "HD", codec: "H.264" },
    { channel: "MTV Hits", number: "801", now: "Top 40 Countdown", next: "Pop Throwbacks", progress: 30, health: "stable", cat: "Music", quality: "SD", codec: "H.264" },
    { channel: "Kerrang! TV", number: "805", now: "Rock Anthems", next: "New Noise", progress: 66, health: "moderate", cat: "Music", quality: "SD", codec: "H.264" },
    { channel: "Sky Cinema Premiere", number: "301", now: "Dune: Part Two", next: "Oppenheimer", progress: 12, health: "stable", cat: "Movies", quality: "FHD", codec: "H.265", catchup: true, fav: true },
    { channel: "TCM", number: "315", now: "Casablanca", next: "The Godfather", progress: 80, health: "stable", cat: "Movies", quality: "SD", codec: "H.264" },
    { channel: "TF1", number: "901", now: "Journal de 20h", next: "Koh-Lanta", progress: 25, health: "stable", cat: "International", quality: "HD", codec: "H.265", catchup: true },
    { channel: "RAI 1", number: "915", now: "Telegiornale", next: "Serie A Live", progress: 58, health: "moderate", cat: "International", quality: "HD", codec: "H.264" },
    { channel: "DAZN 4K", number: "600", now: "UFC Fight Night", next: "Boxing Tonight", progress: 42, health: "stable", cat: "4K & UHD", quality: "4K", codec: "H.265", catchup: true, fav: true },
    { channel: "Insight UHD", number: "608", now: "Planet Earth III", next: "Blue Planet", progress: 19, health: "stable", cat: "4K & UHD", quality: "4K", codec: "H.265" },
  ];

  const movies = [
    { title: "Dune: Part Two", meta: "2024 · Sci-Fi", image: poster("dune-two"), rating: "9.1", badge: "4K" },
    { title: "Oppenheimer", meta: "2023 · Drama", image: poster("oppenheimer"), rating: "8.9", badge: "4K" },
    { title: "The Batman", meta: "2022 · Action", image: poster("thebatman"), rating: "8.4" },
    { title: "Poor Things", meta: "2023 · Comedy", image: poster("poorthings"), rating: "8.0", badge: "NEW" },
    { title: "Killers of the Flower Moon", meta: "2023 · Crime", image: poster("kotfm"), rating: "7.8" },
    { title: "Everything Everywhere", meta: "2022 · Sci-Fi", image: poster("eeaao"), rating: "8.1" },
    { title: "Past Lives", meta: "2023 · Drama", image: poster("pastlives"), rating: "8.3" },
    { title: "The Holdovers", meta: "2023 · Comedy", image: poster("holdovers"), rating: "8.0" },
  ];

  const series = [
    { title: "Shōgun", meta: "S1 · Drama", image: poster("shogun-s"), rating: "9.2", badge: "NEW" },
    { title: "The Bear", meta: "S3 · Comedy", image: poster("thebear"), rating: "8.6" },
    { title: "Fallout", meta: "S1 · Sci-Fi", image: poster("fallout"), rating: "8.5", badge: "4K" },
    { title: "True Detective", meta: "S4 · Crime", image: poster("truedetective"), rating: "8.3" },
    { title: "Severance", meta: "S2 · Thriller", image: poster("severance"), rating: "8.7" },
    { title: "The Last of Us", meta: "S2 · Drama", image: poster("tlou-s"), rating: "8.8" },
    { title: "Slow Horses", meta: "S4 · Thriller", image: poster("slowhorses-s"), rating: "8.3" },
    { title: "Ripley", meta: "S1 · Crime", image: poster("ripley"), rating: "8.2" },
  ];

  const recommended = [
    { title: "MotoGP Highlights", meta: "Because you watch F1", image: poster("motogp"), rating: "—", smart: true },
    { title: "Senna", meta: "Docuseries", image: poster("senna"), rating: "8.9", smart: true },
    { title: "Ford v Ferrari", meta: "2019 · Drama", image: poster("fvf"), rating: "8.1", smart: true },
    { title: "Rush", meta: "2013 · Drama", image: poster("rush"), rating: "8.1", smart: true },
    { title: "Gran Turismo", meta: "2023 · Action", image: poster("gt"), rating: "7.2", smart: true },
    { title: "Le Mans '66", meta: "Docs", image: poster("lemans"), rating: "7.9", smart: true },
  ];

  // EPG grid: channels x programs. times are minute-offsets in a 6h window from 18:00.
  const epgChannels = [
    { name: "BBC One HD", num: "101", logo: "BBC", cat: "Entertainment" },
    { name: "ITV1 HD", num: "103", logo: "ITV", cat: "Entertainment" },
    { name: "Channel 4 HD", num: "104", logo: "C4", cat: "Entertainment" },
    { name: "Sky Sports F1", num: "406", logo: "F1", cat: "Sports" },
    { name: "Sky Sports Main", num: "401", logo: "SKY", cat: "Sports" },
    { name: "ESPN", num: "204", logo: "E", cat: "Sports" },
    { name: "CNN", num: "311", logo: "CNN", cat: "News" },
    { name: "Discovery", num: "520", logo: "DIS", cat: "Documentary" },
    { name: "National Geographic", num: "525", logo: "NG", cat: "Documentary" },
    { name: "Cartoon Network", num: "701", logo: "CN", cat: "Kids" },
  ];
  const epgRows = [
    [["18:00", "Regional News", 60, "catchup"], ["19:00", "EastEnders", 30], ["19:30", "The One Show", 30], ["20:00", "The News at Ten", 45, "now"], ["20:45", "Match of the Day Live", 105]],
    [["18:00", "ITV Evening News", 60, "catchup"], ["19:00", "Emmerdale", 30], ["19:30", "Coronation Street", 30], ["20:00", "Drama Premiere", 90, "now"], ["21:30", "News at Ten", 60]],
    [["18:00", "The Simpsons", 60, "catchup"], ["19:00", "Hollyoaks", 30], ["19:30", "Grand Designs", 60, "now"], ["20:30", "Feature Film: Arrival", 120]],
    [["18:00", "F1 Practice 3", 90, "catchup"], ["19:30", "Qualifying Analysis", 30], ["20:00", "British Grand Prix", 150, "now"], ["22:30", "Post-race", 60]],
    [["18:00", "Premier League Review", 60], ["19:00", "Live Football", 120, "now"], ["21:00", "Gillette Soccer", 60], ["22:00", "Highlights", 60]],
    [["18:00", "SportsCenter", 60, "catchup"], ["19:00", "NBA Pregame", 30], ["19:30", "NBA: Lakers @ Celtics", 150, "now"], ["22:00", "Post-game", 60]],
    [["18:00", "Global News Hour", 60, "now"], ["19:00", "Amanpour", 60], ["20:00", "Quest Means Business", 60], ["21:00", "CNN Newsroom", 120]],
    [["18:00", "How It's Made", 60, "catchup"], ["19:00", "Gold Rush", 60, "now"], ["20:00", "Deadliest Catch", 60], ["21:00", "Expedition Unknown", 120]],
    [["18:00", "Wild Planet", 90, "catchup"], ["19:30", "Drain the Oceans", 60, "now"], ["20:30", "Air Crash Investigation", 90], ["22:00", "Cosmos", 60]],
    [["18:00", "Adventure Time", 60, "catchup"], ["19:00", "Regular Show", 30], ["19:30", "Gumball", 30, "now"], ["20:00", "Teen Titans Go!", 60], ["21:00", "We Bare Bears", 60]],
  ];

  const categories = ["All", "Sports", "Movies", "News", "Kids", "Entertainment", "Documentary", "Music", "International"];

  // ---- CATEGORIES: the grouping every content type shares (Live TV, Movies, Series, EPG) ----
  // Real playlists often ship NO artwork per category, so most cats are artless
  // (the CategoryCard renders a clean kind-icon folder). VOD cats borrow real
  // poster art only where matched items actually exist.
  const genreOf = (m) => (m.meta.split("\u00b7")[1] || "").trim();
  const pics = (list, match) => list.filter(x => genreOf(x).includes(match)).map(x => x.image);
  const anyPics = (list, n) => list.slice(0, n).map(x => x.image);

  // VOD category columns — `match` filters the grid by genre keyword; `count` is display-realistic.
  const movieCats = [
    { name: "All movies", kind: "movies", count: 642, posters: anyPics(movies, 4), all: true },
    { name: "Recently added", kind: "movies", count: 48, posters: anyPics(movies.slice(3), 4), smart: true, recent: true },
    { name: "Action & Adventure", kind: "movies", count: 128, posters: pics(movies, "Action"), match: "Action" },
    { name: "Sci-Fi & Fantasy", kind: "movies", count: 96, posters: pics(movies, "Sci-Fi"), match: "Sci-Fi" },
    { name: "Drama", kind: "movies", count: 210, posters: pics(movies, "Drama"), match: "Drama" },
    { name: "Comedy", kind: "movies", count: 84, posters: pics(movies, "Comedy"), match: "Comedy" },
    { name: "Crime & Thriller", kind: "movies", count: 72, posters: pics(movies, "Crime"), match: "Crime" },
    { name: "Documentary", kind: "movies", count: 61, match: "Documentary" },
    { name: "Family & Kids", kind: "movies", count: 118, match: "Family" },
    { name: "Horror", kind: "movies", count: 54, match: "Horror" },
    { name: "Romance", kind: "movies", count: 39, match: "Romance" },
  ];
  const seriesCats = [
    { name: "All series", kind: "series", count: 318, posters: anyPics(series, 4), all: true },
    { name: "Recently added", kind: "series", count: 22, posters: anyPics(series.slice(2), 4), smart: true, recent: true },
    { name: "Drama", kind: "series", count: 96, posters: pics(series, "Drama"), match: "Drama" },
    { name: "Sci-Fi & Fantasy", kind: "series", count: 41, posters: pics(series, "Sci-Fi"), match: "Sci-Fi" },
    { name: "Crime", kind: "series", count: 63, posters: pics(series, "Crime"), match: "Crime" },
    { name: "Comedy", kind: "series", count: 38, posters: pics(series, "Comedy"), match: "Comedy" },
    { name: "Thriller", kind: "series", count: 57, posters: pics(series, "Thriller"), match: "Thriller" },
    { name: "Reality & Docs", kind: "series", count: 44, match: "Reality" },
    { name: "Animation", kind: "series", count: 31, match: "Animation" },
  ];

  // Live TV category folders — channel groups. Artless (real IPTV groups rarely ship art).
  const liveCats = [
    { name: "All channels", kind: "live", count: 1240, all: true },
    { name: "Sports", kind: "live", count: 128 },
    { name: "News", kind: "live", count: 64 },
    { name: "Entertainment", kind: "live", count: 312 },
    { name: "Kids", kind: "live", count: 39 },
    { name: "Documentary", kind: "live", count: 57 },
    { name: "Movies", kind: "live", count: 210 },
    { name: "Music", kind: "live", count: 44 },
    { name: "International", kind: "live", count: 486 },
    { name: "4K & UHD", kind: "live", count: 72 },
    { name: "Local channels", kind: "live", count: 28 },
    { name: "24/7 & VOD channels", kind: "live", count: 96 },
  ];

  // Curated "Browse by category" rail for Home — a mix across content types.
  const browseCats = [
    { name: "Sports", kind: "live", count: 128 },
    { name: "Action & Adventure", kind: "movies", count: 128, posters: pics(movies, "Action") },
    { name: "Drama series", kind: "series", count: 96, posters: pics(series, "Drama") },
    { name: "News", kind: "live", count: 64 },
    { name: "Suggested for you", kind: "movies", count: 40, posters: anyPics(recommended, 4), smart: true },
    { name: "Kids", kind: "live", count: 39 },
    { name: "Documentary", kind: "guide", count: 57 },
    { name: "Catch-up", kind: "catchup", count: 12 },
  ];

  const detail = {
    title: "Shōgun", year: "2024", genres: "Drama · History · Adventure", seasons: "1 season · 10 episodes",
    rating: "9.2", maturity: "18", quality: "4K HDR",
    synopsis: "Set in Japan in the year 1600, a power struggle unfolds as Lord Yoshii Toranaga fights for survival against his rivals on the Council of Regents, while an English sailor's arrival upends the balance of power.",
    cast: ["Hiroyuki Sanada", "Cosmo Jarvis", "Anna Sawai", "Tadanobu Asano", "Takehiro Hira"],
    backdrop: wide("shogun-backdrop"), poster: poster("shogun-detail"),
    episodes: [
      { n: 1, title: "Anjin", dur: "58m", still: still("shogun-e1"), desc: "A European ship runs aground off a fishing village." },
      { n: 2, title: "Servants of Two Masters", dur: "56m", still: still("shogun-e2"), desc: "Toranaga navigates the council's hostility." },
      { n: 3, title: "Tomorrow Is Tomorrow", dur: "52m", still: still("shogun-e3"), desc: "An escape from Osaka Castle turns deadly." },
      { n: 4, title: "The Eightfold Fence", dur: "54m", still: still("shogun-e4"), desc: "Blackthorne adjusts to life in Ajiro." },
    ],
  };

  return { img, poster, still, wide, featured, continueWatching, liveNow, liveChannelPool, movies, series, recommended, epgChannels, epgRows, categories, movieCats, seriesCats, liveCats, browseCats, genreOf, detail };
})();
