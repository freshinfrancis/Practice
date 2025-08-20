// lib/screens/chat_screen.dart
import 'package:flutter/material.dart';
import '../api/livewell_api.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key, required this.userId});
  final String userId;

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final _api = LiveWellApi();
  final _ctrl = TextEditingController();
  bool _busy = false;

  // very simple message model
  final List<_Msg> _messages = <_Msg>[
    _Msg(fromUser: false, text: "Hi! How are you today?"),
  ];

  Prisma7? _lastPrisma; // optional: stash last prisma answers

  Future<void> _send() async {
    final text = _ctrl.text.trim();
    if (text.isEmpty || _busy) return;
    setState(() {
      _messages.add(_Msg(fromUser: true, text: text));
      _busy = true;
    });
    _ctrl.clear();

    try {
      final resp = await _api.chat(
        userId: widget.userId,
        message: text,
        prisma7: _lastPrisma, // include if collected
      );

      final planText = resp.plan.isNotEmpty
          ? "\n\nToday's plan:\n${resp.plan.map((p) => "• ${p.title}: ${p.description}").join("\n")}"
          : "";

      final frailtyText = resp.frailty != null
          ? "\n\nFrailty: score ${resp.frailty!.score} (${resp.frailty!.band})"
          : "";

      setState(() {
        _messages.add(_Msg(fromUser: false, text: "${resp.reply}$planText$frailtyText"));
      });
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Error: $e")),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _openPrismaSheet() async {
    final result = await showModalBottomSheet<Prisma7>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const _PrismaForm(),
    );
    if (result != null) {
      setState(() => _lastPrisma = result);
      // (optional) immediately score and show a toast
      try {
        final score = await _api.scorePrisma7(result);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text("PRISMA-7: score ${score['score']} (${score['band']})")),
          );
        }
      } catch (_) {}
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("LiveWell-E"),
        actions: [
          IconButton(
            tooltip: "PRISMA-7",
            onPressed: _busy ? null : _openPrismaSheet,
            icon: const Icon(Icons.assignment_turned_in),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: _messages.length,
              itemBuilder: (_, i) {
                final m = _messages[i];
                final align = m.fromUser ? CrossAxisAlignment.end : CrossAxisAlignment.start;
                final color = m.fromUser ? Colors.purple.shade100 : Colors.grey.shade200;
                return Column(
                  crossAxisAlignment: align,
                  children: [
                    Container(
                      margin: const EdgeInsets.symmetric(vertical: 6),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: color,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(m.text),
                    ),
                  ],
                );
              },
            ),
          ),
          SafeArea(
            child: Row(
              children: [
                const SizedBox(width: 8),
                Expanded(
                  child: TextField(
                    controller: _ctrl,
                    decoration: const InputDecoration(
                      hintText: "Type a message…",
                      border: OutlineInputBorder(),
                    ),
                    onSubmitted: (_) => _send(),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton.icon(
                  onPressed: _busy ? null : _send,
                  icon: const Icon(Icons.send),
                  label: const Text("Send"),
                ),
                const SizedBox(width: 8),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Msg {
  final bool fromUser;
  final String text;
  _Msg({required this.fromUser, required this.text});
}

// ---- Minimal PRISMA-7 form ----
class _PrismaForm extends StatefulWidget {
  const _PrismaForm();

  @override
  State<_PrismaForm> createState() => _PrismaFormState();
}

class _PrismaFormState extends State<_PrismaForm> {
  bool over85 = false;
  bool male = false;
  bool limit = false;
  bool needHelp = false;
  bool stayHome = false;
  bool someoneClose = false;
  bool useAid = false;

  @override
  Widget build(BuildContext context) {
    final items = <_Toggle>[
      _Toggle("Are you over 85?", (v) => setState(() => over85 = v), over85),
      _Toggle("Are you male?", (v) => setState(() => male = v), male),
      _Toggle("Do health problems limit your activities?", (v) => setState(() => limit = v), limit),
      _Toggle("Do you need help on a regular basis?", (v) => setState(() => needHelp = v), needHelp),
      _Toggle("Do health problems mean you stay at home?", (v) => setState(() => stayHome = v), stayHome),
      _Toggle("Can you count on someone close to you?", (v) => setState(() => someoneClose = v), someoneClose),
      _Toggle("Do you use a stick, walker, or wheelchair?", (v) => setState(() => useAid = v), useAid),
    ];

    return Padding(
      padding: EdgeInsets.only(
        left: 16, right: 16,
        top: 16,
        bottom: 16 + MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text("PRISMA-7", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...items.map((t) => SwitchListTile(title: Text(t.label), value: t.value, onChanged: t.onChanged)),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: () {
              Navigator.of(context).pop(
                Prisma7(
                  over85: over85,
                  male: male,
                  healthProblemsLimitActivities: limit,
                  needHelpRegularly: needHelp,
                  healthProblemsStayHome: stayHome,
                  countOnSomeoneClose: someoneClose,
                  useStickWalkerWheelchair: useAid,
                ),
              );
            },
            child: const Text("Use in next chat"),
          ),
          const SizedBox(height: 8),
        ],
      ),
    );
  }
}

class _Toggle {
  final String label;
  final ValueChanged<bool> onChanged;
  final bool value;
  _Toggle(this.label, this.onChanged, this.value);
}
