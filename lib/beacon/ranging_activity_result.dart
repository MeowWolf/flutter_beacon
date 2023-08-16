part of flutter_beacon;

class RangingActivityResult {
  final bool isRanging;
  RangingActivityResult.from(dynamic json)
      : isRanging = json['ranging_status'];

  dynamic get toJson => <String, dynamic>{
        'ranging_status': isRanging,
      };
}
