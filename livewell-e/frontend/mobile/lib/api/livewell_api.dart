// lib/api/livewell_api.dart
import 'dart:convert';
import 'dart:io' show Platform; // if you target web later, gate this or use universal_io
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;

// ---------- Base URL picker ----------
class ApiConfig {
  static String baseUrl = _detectBaseUrl();

  static String _detectBaseUrl() {
    if (kIsWeb) return 'http://127.0.0.1:8000'; // adjust if serving web separately
    if (Platform.isAndroid) return 'http://10.0.2.2:8000';
    return 'http://127.0.0.1:8000';
  }
}

// ---------- Data models ----------
class Prisma7 {
  final bool over85;
  final bool male;
  final bool healthProblemsLimitActivities;
  final bool needHelpRegularly;
  final bool healthProblemsStayHome;
  final bool countOnSomeoneClose;
  final bool useStickWalkerWheelchair;

  Prisma7({
    required this.over85,
    required this.male,
    required this.healthProblemsLimitActivities,
    required this.needHelpRegularly,
    required this.healthProblemsStayHome,
    required this.countOnSomeoneClose,
    required this.useStickWalkerWheelchair,
  });

  Map<String, dynamic> toJson() => {
    "over_85": over85,
    "male": male,
    "health_problems_limit_activities": healthProblemsLimitActivities,
    "need_help_regularly": needHelpRegularly,
    "health_problems_stay_home": healthProblemsStayHome,
    "count_on_someone_close": countOnSomeoneClose,
    "use_stick_walker_wheelchair": useStickWalkerWheelchair,
  };
}

class PlanItem {
  final String title;
  final String description;
  final int? durationMinutes;

  PlanItem({required this.title, required this.description, this.durationMinutes});
  factory PlanItem.fromJson(Map<String, dynamic> j) => PlanItem(
    title: j["title"] ?? "",
    description: j["description"] ?? "",
    durationMinutes: j["duration_minutes"],
  );
}

class Frailty {
  final int score;
  final String band;
  Frailty({required this.score, required this.band});
  factory Frailty.fromJson(Map<String, dynamic> j) =>
      Frailty(score: j["score"] ?? 0, band: j["band"] ?? "low");
}

class ChatResponse {
  final String reply;
  final List<PlanItem> plan;
  final Frailty? frailty;

  ChatResponse({required this.reply, required this.plan, this.frailty});

  factory ChatResponse.fromJson(Map<String, dynamic> j) {
    final planList = (j["plan"] as List? ?? [])
        .whereType<Map<String, dynamic>>()
        .map((e) => PlanItem.fromJson(e))
        .toList();
    final fr = j["frailty"] != null ? Frailty.fromJson(j["frailty"]) : null;
    return ChatResponse(reply: j["reply"] ?? "", plan: planList, frailty: fr);
  }
}

// ---------- Service ----------
class LiveWellApi {
  LiveWellApi({String? base}) : _base = base ?? ApiConfig.baseUrl;
  final String _base;
  final _headers = const {"Content-Type": "application/json"};

  Future<Map<String, dynamic>> scorePrisma7(Prisma7 p) async {
    final res = await http.post(
      Uri.parse("$_base/assessments/prisma7"),
      headers: _headers,
      body: jsonEncode(p.toJson()),
    );
    if (res.statusCode >= 400) {
      throw Exception("PRISMA-7 failed: ${res.statusCode} ${res.body}");
    }
    return jsonDecode(res.body) as Map<String, dynamic>;
  }

  Future<ChatResponse> chat({
    required String userId,
    required String message,
    Prisma7? prisma7,
  }) async {
    final payload = {
      "user_id": userId,
      "message": message,
      if (prisma7 != null) "prisma7": prisma7.toJson(),
    };
    final res = await http.post(
      Uri.parse("$_base/chat"),
      headers: _headers,
      body: jsonEncode(payload),
    );
    if (res.statusCode >= 400) {
      throw Exception("Chat failed: ${res.statusCode} ${res.body}");
    }
    return ChatResponse.fromJson(jsonDecode(res.body));
  }
}
