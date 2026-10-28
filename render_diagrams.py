"""Render PNG previews straight from the BPMN diagram data. Run from the repo root."""
import textwrap, xml.etree.ElementTree as ET
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Circle, Polygon, FancyArrowPatch

B = "{http://www.omg.org/spec/BPMN/20100524/MODEL}"
BD = "{http://www.omg.org/spec/BPMN/20100524/DI}"
DC = "{http://www.omg.org/spec/DD/20100524/DC}"
DI = "{http://www.omg.org/spec/DD/20100524/DI}"

STYLE = {  # fill, edge, caption
    "userTask": ("#DCEBFF", "#2E6DB4", "User Task"),
    "serviceTask": ("#DFF5DF", "#3C8C3C", "REST Outbound Connector"),
    "scriptTask": ("#FFF6D6", "#B58B00", "Script Task"),
    "businessRuleTask": ("#F0E4FF", "#7A3CB5", "Business Rule Task (DMN)"),
    "callActivity": ("#FFFFFF", "#1F1F1F", "Call Activity: send-notification"),
}

def render(path, out, title):
    root = ET.parse(path).getroot()
    el = {e.get("id"): e for e in root.iter() if e.get("id")}
    fig, ax = plt.subplots()
    shapes = list(root.iter(BD + "BPMNShape"))
    # draw the event-subprocess frame first so contents sit on top
    shapes.sort(key=lambda s: el[s.get("bpmnElement")].tag != B + "subProcess")
    xs, ys = [], []
    for s in shapes:
        e = el[s.get("bpmnElement")]; b = s.find(DC + "Bounds")
        x, y, w, h = (float(b.get(k)) for k in ("x", "y", "width", "height"))
        xs += [x, x + w]; ys += [y, y + h]
        tag = e.tag.replace(B, ""); name = e.get("name", ""); cx, cy = x + w / 2, -(y + h / 2)
        if tag == "subProcess":
            ax.add_patch(FancyBboxPatch((x, -(y + h)), w, h, boxstyle="round,pad=0,rounding_size=10", fc="#FBFBFB",
                                         ec="#555", lw=1.4, ls=(0, (5, 3)), zorder=1))
            ax.text(x + 10, -y - 22, "Event subprocess: " + name + "\n(interrupting \u2709 cancel_booking, key = patientId)",
                    fontsize=8.5, va="center", fontweight="bold", color="#444")
        elif tag in ("startEvent", "endEvent"):
            msg = e.find(B + "messageEventDefinition") is not None
            ax.add_patch(Circle((cx, cy), w / 2, fc="#DFF5DF" if tag == "startEvent" else "#FFE0E0",
                                ec="#3C8C3C" if tag == "startEvent" else "#B03A3A", lw=3 if tag == "endEvent" else 2,
                                ls="-" , zorder=3))
            if msg: ax.text(cx, cy, "\u2709", ha="center", va="center", fontsize=11, zorder=4)
            ax.text(cx, cy - h / 2 - 8, "\n".join(textwrap.wrap(name, 18)), fontsize=7.8, ha="center", va="top")
        elif tag == "intermediateCatchEvent":
            ax.add_patch(Circle((cx, cy), w / 2, fc="#FFF6D6", ec="#B58B00", lw=2, zorder=3))
            ax.add_patch(Circle((cx, cy), w / 2 - 3.5, fc="none", ec="#B58B00", lw=1.2, zorder=3))
            if e.find(B + "timerEventDefinition") is not None:  # clock hands
                ax.add_patch(Circle((cx, cy), 8, fc="white", ec="#B58B00", lw=1, zorder=4))
                ax.plot([cx, cx], [cy, cy + 6], color="#B58B00", lw=1.2, zorder=5)
                ax.plot([cx, cx + 4.5], [cy, cy], color="#B58B00", lw=1.2, zorder=5)
            ax.text(cx, cy - h / 2 - 8, "\n".join(textwrap.wrap(name, 16)), fontsize=7.8, ha="center", va="top")
        elif tag == "exclusiveGateway":
            ax.add_patch(Polygon([(cx, cy + h / 2), (cx + w / 2, cy), (cx, cy - h / 2), (cx - w / 2, cy)],
                                 fc="#FFF3D6", ec="#B58B00", lw=2, zorder=3))
            ax.text(cx, cy, "X", ha="center", va="center", fontsize=11, fontweight="bold", color="#B58B00", zorder=4)
            ax.text(cx, cy + h / 2 + 8, name, fontsize=8, ha="center", va="bottom", fontweight="bold")
        else:
            fc, ec, cap = STYLE.get(tag, ("#EEE", "#555", tag))
            ax.add_patch(FancyBboxPatch((x, -(y + h)), w, h, boxstyle="round,pad=0,rounding_size=7", fc=fc, ec=ec,
                                         lw=3.2 if tag == "callActivity" else 1.6, zorder=3))
            ax.text(cx, cy + 9, "\n".join(textwrap.wrap(name, 17)), fontsize=8, ha="center", va="center", zorder=4)
            ax.text(cx, cy - h / 2 + 8, cap, fontsize=5.8, ha="center", va="bottom", color=ec, style="italic", zorder=4)
            if tag == "callActivity":
                ax.text(cx, cy - h / 2 + 1, "+", fontsize=10, ha="center", va="bottom", color=ec, fontweight="bold", zorder=4)
    for ed in root.iter(BD + "BPMNEdge"):
        pts = [(float(w.get("x")), -float(w.get("y"))) for w in ed.findall(DI + "waypoint")]
        for i in range(len(pts) - 1):
            last = i == len(pts) - 2
            ax.add_patch(FancyArrowPatch(pts[i], pts[i + 1], arrowstyle="-|>" if last else "-", mutation_scale=13,
                                         color="#555", lw=1.4, shrinkA=0, shrinkB=0, zorder=2))
        nm = el[ed.get("bpmnElement")].get("name")
        if nm:
            (x1, y1), (x2, y2) = pts[0], pts[1]
            horiz = abs(y2 - y1) < 1
            ax.text((x1 + x2) / 2 + (0 if horiz else 24), (y1 + y2) / 2 + (10 if horiz else 0), nm, fontsize=8,
                    ha="center", va="center", fontweight="bold", color="#8a4b00",
                    bbox=dict(boxstyle="round,pad=0.2", fc="#FFFDF5", ec="#B58B00", lw=0.6), zorder=5)
    ax.set_xlim(min(xs) - 40, max(xs) + 50); ax.set_ylim(-max(ys) - 50, -min(ys) + 55)
    fig.set_size_inches(max(7, (max(xs) - min(xs) + 90) / 75), max(2.8, (max(ys) - min(ys) + 105) / 75 + 0.8))
    ax.set_aspect("equal"); ax.axis("off")
    ax.set_title(title, fontsize=13, fontweight="bold", pad=10)
    fig.tight_layout(); fig.savefig(out, dpi=150, bbox_inches="tight"); plt.close(fig)
    print("wrote", out)

render("models/hospital-booking-propagate-all.bpmn", "diagrams/parent_process.png",
       "Hospital Appointment Booking \u2014 parent process")
render("models/send-notification.bpmn", "diagrams/send_notification.png",
       "send-notification \u2014 reusable child process")
