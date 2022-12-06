mod aoc_client;

use std::collections::HashSet;

fn main() {
    let data = aoc_client::input(2022, 6).unwrap();
    let bytes = data.as_bytes();
    let all_unique = |b: &[u8]| b.iter().collect::<HashSet<_>>().len() == b.len();

    let first_marker = bytes.windows(4).position(all_unique);
    // Output the byte _after_ the marker is complete
    println!("{}", first_marker.unwrap() + 4);

    let first_start_of_message_marker = bytes.windows(14).position(all_unique);
    // Output the byte _after_ the marker is complete
    println!("{}", first_start_of_message_marker.unwrap() + 14);
}
