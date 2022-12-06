mod aoc_client;

use std::collections::HashSet;

fn main() {
    let data = aoc_client::input(2022, 6).unwrap();
    // You LITERALLY CANNOT TREAT A STRING AS A BYTE SLICE.
    let bytes: Vec<u8> = data.bytes().collect();
    let all_unique = |b: &[u8]| HashSet::<u8>::from_iter(b.iter().cloned()).len() == b.len();
    let first_marker = bytes.windows(4).enumerate().find(|(_, v)| all_unique(v));
    // Output the byte _after_ the marker is complete
    println!("{}", first_marker.unwrap().0 + 4);

    let first_start_of_message_marker = bytes.windows(14).enumerate().find(|(_, v)| all_unique(v));
    // Output the byte _after_ the marker is complete
    println!("{}", first_start_of_message_marker.unwrap().0 + 14);
}
