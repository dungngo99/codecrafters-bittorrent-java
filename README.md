<h1>BitTorrent Java — Codecrafters Challenge</h1>

<p>
  A minimal BitTorrent client written in Java as part of the
  <a href="https://codecrafters.io" target="_blank" rel="noopener noreferrer">Codecrafters</a> series.
  This project explores Bencoding, tracker communication, peer handshakes, and length-prefixed message streams.
</p>

<hr />

<h2>Contents</h2>
<ol>
  <li><a href="#introduction">Introduction</a>
    <ol>
      <li><a href="#what-is-bittorrent">What is BitTorrent?</a></li>
      <li><a href="#why-bittorrent">Why do we need BitTorrent?</a></li>
      <li><a href="#overall-architecture">Overall Architecture (5 main components)</a></li>
      <li><a href="#use-cases">Use cases</a></li>
    </ol>
  </li>
  <li><a href="#details">Details</a>
    <ol>
      <li><a href="#bencoding">B-encoded protocols</a></li>
      <li><a href="#codecrafters-test">Codecrafters test</a></li>
      <li><a href="#downloading-and-sharing">Download &amp; sharing</a></li>
      <li><a href="#creating-and-uploading">Creating &amp; uploading</a></li>
      <li><a href="#anonymity">Anonymity</a></li>
      <li><a href="#choke-algorithms">Choke algorithms</a></li>
    </ol>
  </li>
  <li><a href="#conclusion">Conclusion</a></li>
</ol>

<hr />

<h2 id="introduction">1. Introduction</h2>

<h3 id="what-is-bittorrent">1.1 What is BitTorrent?</h3>
<p>
  <strong>BitTorrent</strong> is a peer-to-peer (P2P) file distribution protocol for transferring large files efficiently.
  Instead of using a single server, peers exchange pieces of the file directly with one another, improving scalability and resilience.
</p>

<h3 id="why-bittorrent">1.2 Why do we need BitTorrent?</h3>
<p>
  Centralized downloads (HTTP/FTP) are limited by the server’s upload bandwidth. If computer A can download at 100&nbsp;Mbps
  but the server (B) uploads at 60&nbsp;Mbps, A is effectively capped at 60&nbsp;Mbps. BitTorrent splits a file into many chunks
  (e.g., <code>[C1, C2, C3, C4, C5]</code>) and lets a new peer fetch different chunks from different peers concurrently
  (<code>[A1, A2, A3, A4, A5]</code>), achieving higher aggregate throughput and better fault tolerance.
</p>

<h3 id="overall-architecture">1.3 Overall Architecture</h3>

<p align="center">
  <!-- If you later host a direct-viewable image, swap the href/src below to the raw image URL -->
  <a href="https://drive.google.com/file/d/1ty3ZBc-cRgY-uKmC1RAG48La4xjETAHq/view?usp=sharing" target="_blank" rel="noopener noreferrer">
    <img src="https://drive.google.com/file/d/1ty3ZBc-cRgY-uKmC1RAG48La4xjETAHq/preview"
         alt="BitTorrent Architecture Diagram (click to open in Google Drive)"
         style="max-width: 720px; width: 100%; border: 1px solid #e5e7eb; border-radius: 8px;" />
  </a>
</p>

<ol>
  <li><strong>Seeder</strong> (file owner) publishes torrent metadata (<code>.torrent</code>) to an index and registers with a tracker.</li>
  <li>Search/index servers periodically sync new torrents for discovery.</li>
  <li>A <strong>leecher</strong> downloads the <code>.torrent</code> file from an index/search client.</li>
  <li>The leecher contacts the tracker to obtain peer endpoints (IP:port).</li>
  <li>The leecher downloads multiple pieces in parallel from peers.</li>
  <li>After completing a piece, the leecher announces availability to the tracker and may start seeding that piece.</li>
</ol>

<h4>Five main components</h4>
<table>
  <thead>
    <tr>
      <th align="left">Component</th>
      <th align="left">Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><strong>Tracker</strong></td>
      <td>Coordinates peer lists and who owns which pieces. It manages connections only (not file data), enabling large swarms with limited server bandwidth.</td>
    </tr>
    <tr>
      <td><strong>Index</strong></td>
      <td>Searchable catalog of <code>.torrent</code> files, similar to a metadata directory for discovery.</td>
    </tr>
    <tr>
      <td><strong>Data</strong></td>
      <td>Raw content (byte stream) and the <code>.torrent</code> metadata (B-encoded key–value pairs like <code>announce</code>, <code>info</code>, <code>created by</code>).</td>
    </tr>
    <tr>
      <td><strong>Peer</strong></td>
      <td>Any node in the swarm acting as a <em>seeder</em> (uploads pieces) or <em>leecher</em> (downloads pieces). Seeder/leecher ratio impacts overall throughput.</td>
    </tr>
    <tr>
      <td><strong>Client</strong></td>
      <td>End-user application (e.g., µTorrent, Transmission, Vuze). Some tools/browsers support torrents natively.</td>
    </tr>
  </tbody>
</table>

<h3 id="use-cases">1.4 Use cases</h3>
<ul>
  <li>Distributing OS images (e.g., Linux distributions) faster than HTTP/FTP.</li>
  <li>Sharing large software/game installers and datasets.</li>
  <li>Propagating patches across many machines in data centers.</li>
  <li>Large-scale internal deployments (e.g., distributing build artifacts across servers).</li>
</ul>

<hr />

<h2 id="details">2. Details</h2>

<h3 id="bencoding">2.1 B-encoded protocols: What is it?</h3>
<p>
  BitTorrent metadata (<code>.torrent</code> files) is encoded using <strong>Bencoding</strong>, a compact, easy-to-parse format.
  Supported types:
</p>

<table>
  <thead>
    <tr>
      <th align="left">Type</th>
      <th align="left">Format</th>
      <th align="left">Example</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>String</td>
      <td><code>&lt;length&gt;:&lt;string&gt;</code></td>
      <td><code>dylan → 5:dylan</code></td>
    </tr>
    <tr>
      <td>Integer</td>
      <td><code>i&lt;integer&gt;e</code></td>
      <td><code>10 → i10e</code></td>
    </tr>
    <tr>
      <td>List</td>
      <td><code>l&lt;values&gt;e</code></td>
      <td><code>[&quot;a&quot;,&quot;b&quot;,1] → l1:a1:bi1ee</code></td>
    </tr>
    <tr>
      <td>Dictionary</td>
      <td><code>d&lt;key&gt;&lt;value&gt;e</code></td>
      <td><code>{a:1,b:2} → da:1i1eb:1i2ee</code></td>
    </tr>
  </tbody>
</table>

<p>
  Parsing is implemented via a simple recursive descent decoder in this repo. See <code>src/main/java/&hellip;/bencode</code>.
  Background reading: <a href="https://en.wikipedia.org/wiki/Bencode" target="_blank" rel="noopener noreferrer">Bencode (Wikipedia)</a>.
</p>

<h3 id="codecrafters-test">2.2 Codecrafters test</h3>
<ol>
  <li>Implement Bencoder/Bdecoder (recursive parser/serializer).</li>
  <li>Decode a sample <code>.torrent</code> in the repo and extract metadata fields.</li>
  <li>Compute the <strong>info hash</strong> by SHA-1 hashing the <code>info</code> dictionary; parse the piece hashes list.</li>
  <li>Use the <code>announce</code> URL to query the tracker (HTTP GET) and retrieve peer IP/port info.</li>
  <li>Open TCP sockets to peers, perform the BitTorrent <em>handshake</em>, then exchange <strong>length-prefixed</strong> messages to request/receive pieces until the full file is assembled.</li>
</ol>
<p>
  Note: Tracker/client exchanges commonly use B-encoded payloads, while peer-to-peer exchanges use streaming, length-prefixed binary messages (e.g., <em>choke</em>, <em>unchoke</em>, <em>have</em>, <em>request</em>, <em>piece</em>, etc.).
</p>

<h3 id="downloading-and-sharing">2.3 Download &amp; sharing</h3>

<h4>Problem 1: Strict policies</h4>
<ul>
  <li><strong>Background:</strong> Many clients favor tit-for-tat fairness (you send me data, I send you data), which can starve newcomers or stall when neither peer initiates.</li>
  <li><strong>Solution:</strong> <em>Optimistic unchoking</em> — periodically reserve some upload bandwidth to random peers to discover better partners and help newcomers join the swarm.</li>
</ul>

<h4>Problem 2: Seeder promotion</h4>
<ul>
  <li><strong>Background:</strong> Unpopular content risks losing seeders, hurting availability.</li>
  <li><strong>Solutions:</strong>
    <ul>
      <li>Bundle multiple files into a single torrent to increase availability.</li>
      <li>Adopt cross-torrent mechanisms to encourage sharing across related swarms.</li>
    </ul>
  </li>
</ul>

<h3 id="creating-and-uploading">2.4 Creating &amp; uploading</h3>
<ol>
  <li><code>.torrent</code> files are B-encoded dictionaries including <code>announce</code>, <code>info</code> (name, length, piece hashes), and optional fields like <code>comment</code>, <code>created by</code>, <code>creation date</code>.</li>
  <li>Traditional flow: publish the torrent to an index and register with one or more trackers.</li>
  <li>Modern/alternative: <strong>trackerless</strong> swarms using <a href="https://en.wikipedia.org/wiki/Distributed_hash_table" target="_blank" rel="noopener noreferrer">DHT</a>, where peers collaboratively provide “tracker” functionality.</li>
</ol>

<h3 id="anonymity">2.5 Anonymity</h3>
<p>
  BitTorrent does <em>not</em> provide built-in anonymity. Peers can view each other’s IP addresses
  (via the client UI or firewall). Users often rely on VPNs or other privacy tools if needed, with potential performance trade-offs.
</p>

<h3 id="choke-algorithms">2.6 Choke algorithms</h3>
<p>
  The <strong>Choke/Unchoke</strong> mechanism aims to maintain healthy reciprocation between peers.
  Clients prefer uploading to peers that reciprocate (good throughput), and periodically <em>optimistically unchoke</em> others
  to probe for better connections and avoid starvation.
</p>

<hr />

<h2 id="conclusion">3. Conclusion</h2>
<p>
  This project implements the essentials of a BitTorrent client: Bencoding, tracker interaction, peer handshake,
  and piece exchange over length-prefixed streams. It’s a practical way to learn recursive parsing, TCP socket programming,
  custom binary protocols, and swarm-level coordination strategies.
</p>
<ul>
  <li><strong>Potential next steps:</strong> DHT (trackerless) support, rarest-first piece selection, multi-torrent concurrency, encryption/obfuscation, and richer CLI/GUI.</li>
</ul>

<hr />

<p>
  <em>Diagram:</em> 
  <a href="https://drive.google.com/file/d/1ty3ZBc-cRgY-uKmC1RAG48La4xjETAHq/view?usp=sharing" target="_blank" rel="noopener noreferrer">
    BitTorrent Architecture (Google Drive)
  </a>
</p>
