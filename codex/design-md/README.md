# DESIGN.md Workspace

This folder keeps local design-system references for AI-assisted UI work.

## Layout

- `awesome-design-md/`: local clone of https://github.com/VoltAgent/awesome-design-md.git
- `selected/`: optional place to keep project-approved `DESIGN.md` files
- `../../scripts/design-md.cmd`: helper for listing and installing designs

The cloned upstream repo is intentionally ignored by this project Git repo, so it can be updated or recloned without adding a nested repository to source control.

## List Available Designs

```bat
scripts\design-md.cmd list
```

## Install A Design

Run this from the project root:

```bat
scripts\design-md.cmd add vercel
```

The helper calls:

```bat
npx.cmd getdesign@latest add <brand>
```

The upstream tool writes a `DESIGN.md` file into the current project root. After that, ask your coding agent to use `DESIGN.md` for UI work.

## Update The Local Reference Clone

```bat
scripts\design-md.cmd update
```

## Reclone If Needed

```bat
git clone https://github.com/VoltAgent/awesome-design-md.git codex\design-md\awesome-design-md
```
